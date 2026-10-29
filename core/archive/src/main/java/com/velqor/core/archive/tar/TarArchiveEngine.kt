package com.velqor.core.archive.tar

import com.velqor.core.archive.engine.ArchiveEngine
import com.velqor.core.archive.model.ArchiveEntry
import com.velqor.core.archive.model.ArchiveProgress
import com.velqor.core.common.dispatcher.DispatcherProvider
import com.velqor.core.common.result.AppError
import com.velqor.core.common.result.Result
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale

class TarArchiveEngine(
    private val dispatchers: DispatcherProvider
) : ArchiveEngine {

    override fun canHandle(archivePath: String): Boolean {
        val lower = archivePath.lowercase(Locale.ROOT)
        return lower.endsWith(".tar") || lower.endsWith(".tar.gz") || lower.endsWith(".tgz") ||
               lower.endsWith(".tar.bz2") || lower.endsWith(".tbz2") ||
               lower.endsWith(".tar.xz") || lower.endsWith(".txz")
    }

    private fun openTarStream(file: File): TarArchiveInputStream {
        val fis = FileInputStream(file)
        val bis = BufferedInputStream(fis)
        val lower = file.name.lowercase(Locale.ROOT)
        val stream: InputStream = when {
            lower.endsWith(".tar.gz") || lower.endsWith(".tgz") -> GzipCompressorInputStream(bis)
            lower.endsWith(".tar.bz2") || lower.endsWith(".tbz2") -> BZip2CompressorInputStream(bis)
            lower.endsWith(".tar.xz") || lower.endsWith(".txz") -> XZCompressorInputStream(bis)
            else -> bis
        }
        return TarArchiveInputStream(stream)
    }

    override suspend fun listEntries(archivePath: String, password: String?): Result<List<ArchiveEntry>> = withContext(dispatchers.io) {
        try {
            val file = File(archivePath)
            if (!file.exists()) return@withContext Result.Failure(AppError.FileNotFound())

            val list = mutableListOf<ArchiveEntry>()
            openTarStream(file).use { tarIn ->
                var entry: TarArchiveEntry?
                while (tarIn.nextEntry.also { entry = it } != null) {
                    val e = entry ?: break
                    list.add(
                        ArchiveEntry(
                            path = e.name,
                            name = e.name.trimEnd('/').substringAfterLast('/'),
                            isDirectory = e.isDirectory,
                            uncompressedSize = e.size,
                            compressedSize = e.size,
                            isEncrypted = false,
                            lastModifiedMs = e.modTime?.time ?: 0L
                        )
                    )
                }
            }
            Result.Success(list)
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown("Lỗi đọc tệp TAR: ${e.message}", e))
        }
    }

    override suspend fun extract(
        archivePath: String,
        destinationDir: String,
        selectedEntries: List<String>?,
        password: String?,
        onProgress: ((ArchiveProgress) -> Unit)?
    ): Result<Unit> = withContext(dispatchers.io) {
        try {
            val file = File(archivePath)
            if (!file.exists()) return@withContext Result.Failure(AppError.FileNotFound())

            val dest = File(destinationDir)
            if (!dest.exists()) dest.mkdirs()

            val selectedSet = selectedEntries?.toSet()
            var processed = 0

            openTarStream(file).use { tarIn ->
                var entry: TarArchiveEntry?
                while (tarIn.nextEntry.also { entry = it } != null) {
                    val e = entry ?: break
                    processed++

                    if (selectedSet != null && !selectedSet.contains(e.name)) {
                        continue
                    }

                    val destFile = try {
                        validateDestinationPath(dest, e.name)
                    } catch (se: SecurityException) {
                        return@withContext Result.Failure(AppError.ZipSlipAttackDetected(se.message ?: "Zip Slip"))
                    }

                    if (e.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        FileOutputStream(destFile).use { out ->
                            val buffer = ByteArray(32 * 1024)
                            var count: Int
                            while (tarIn.read(buffer).also { count = it } >= 0) {
                                out.write(buffer, 0, count)
                            }
                        }
                    }

                    onProgress?.invoke(
                        ArchiveProgress(
                            currentEntry = e.name,
                            entriesProcessed = processed,
                            bytesProcessed = e.size
                        )
                    )
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown("Lỗi giải nén TAR: ${e.message}", e))
        }
    }

    override suspend fun createArchive(
        archivePath: String,
        sourcePaths: List<String>,
        password: String?,
        compressionLevel: Int,
        onProgress: ((ArchiveProgress) -> Unit)?
    ): Result<Unit> = withContext(dispatchers.io) {
        try {
            val targetFile = File(archivePath)
            targetFile.parentFile?.mkdirs()

            val fos = FileOutputStream(targetFile)
            val bos = BufferedOutputStream(fos)
            val isGz = archivePath.endsWith(".tar.gz", ignoreCase = true) || archivePath.endsWith(".tgz", ignoreCase = true)

            val outStream = if (isGz) GzipCompressorOutputStream(bos) else bos
            TarArchiveOutputStream(outStream).use { tarOut ->
                tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
                for (path in sourcePaths) {
                    val src = File(path)
                    if (!src.exists()) continue
                    addFileToTar(tarOut, src, "")
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown("Lỗi tạo tệp TAR: ${e.message}", e))
        }
    }

    private fun addFileToTar(tarOut: TarArchiveOutputStream, file: File, base: String) {
        val entryName = if (base.isEmpty()) file.name else "$base/${file.name}"
        val tarEntry = TarArchiveEntry(file, entryName)
        tarOut.putArchiveEntry(tarEntry)

        if (file.isFile) {
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(32 * 1024)
                var count: Int
                while (fis.read(buffer).also { count = it } >= 0) {
                    tarOut.write(buffer, 0, count)
                }
            }
            tarOut.closeArchiveEntry()
        } else if (file.isDirectory) {
            tarOut.closeArchiveEntry()
            file.listFiles()?.forEach { child ->
                addFileToTar(tarOut, child, entryName)
            }
        }
    }

    override suspend fun testIntegrity(archivePath: String, password: String?): Result<Boolean> = withContext(dispatchers.io) {
        try {
            val file = File(archivePath)
            if (!file.exists()) return@withContext Result.Failure(AppError.FileNotFound())
            openTarStream(file).use { tarIn ->
                Result.Success(tarIn.nextEntry != null)
            }
        } catch (e: Exception) {
            Result.Failure(AppError.CorruptedArchive("Kiểm tra toàn vẹn TAR thất bại: ${e.message}", e))
        }
    }
}
