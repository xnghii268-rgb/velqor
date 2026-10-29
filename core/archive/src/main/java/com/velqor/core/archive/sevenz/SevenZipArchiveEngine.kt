package com.velqor.core.archive.sevenz

import com.velqor.core.archive.engine.ArchiveEngine
import com.velqor.core.archive.model.ArchiveEntry
import com.velqor.core.archive.model.ArchiveProgress
import com.velqor.core.common.dispatcher.DispatcherProvider
import com.velqor.core.common.result.AppError
import com.velqor.core.common.result.Result
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class SevenZipArchiveEngine(
    private val dispatchers: DispatcherProvider
) : ArchiveEngine {

    override fun canHandle(archivePath: String): Boolean {
        return archivePath.endsWith(".7z", ignoreCase = true)
    }

    override suspend fun listEntries(archivePath: String, password: String?): Result<List<ArchiveEntry>> = withContext(dispatchers.io) {
        try {
            val file = File(archivePath)
            if (!file.exists()) return@withContext Result.Failure(AppError.FileNotFound())

            val pwdBytes = password?.toByteArray()
            val sevenZFile = if (pwdBytes != null) SevenZFile.builder().setFile(file).setPassword(pwdBytes).get()
                             else SevenZFile.builder().setFile(file).get()

            sevenZFile.use { archive ->
                val list = mutableListOf<ArchiveEntry>()
                for (entry in archive.entries) {
                    list.add(
                        ArchiveEntry(
                            path = entry.name,
                            name = entry.name.trimEnd('/').substringAfterLast('/'),
                            isDirectory = entry.isDirectory,
                            uncompressedSize = entry.size,
                            compressedSize = entry.compressedSize,
                            isEncrypted = entry.hasStream(),
                            lastModifiedMs = entry.lastModifiedDate?.time ?: 0L
                        )
                    )
                }
                Result.Success(list)
            }
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown("Lỗi đọc tệp 7z: ${e.message}", e))
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

            val pwdBytes = password?.toByteArray()
            val sevenZFile = if (pwdBytes != null) SevenZFile.builder().setFile(file).setPassword(pwdBytes).get()
                             else SevenZFile.builder().setFile(file).get()

            sevenZFile.use { archive ->
                val entries = archive.entries.toList()
                val total = entries.size
                var processed = 0

                val selectedSet = selectedEntries?.toSet()

                while (true) {
                    val entry = archive.nextEntry ?: break
                    processed++

                    if (selectedSet != null && !selectedSet.contains(entry.name)) {
                        continue
                    }

                    val destFile = try {
                        validateDestinationPath(dest, entry.name)
                    } catch (se: SecurityException) {
                        return@withContext Result.Failure(AppError.ZipSlipAttackDetected(se.message ?: "Zip Slip"))
                    }

                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        FileOutputStream(destFile).use { out ->
                            val buffer = ByteArray(32 * 1024)
                            var count: Int
                            while (archive.read(buffer).also { count = it } >= 0) {
                                out.write(buffer, 0, count)
                            }
                        }
                    }

                    onProgress?.invoke(
                        ArchiveProgress(
                            currentEntry = entry.name,
                            entriesProcessed = processed,
                            totalEntries = total,
                            percent = (processed.toFloat() / total).coerceIn(0f, 1f)
                        )
                    )
                }
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown("Lỗi giải nén 7z: ${e.message}", e))
        }
    }

    override suspend fun createArchive(
        archivePath: String,
        sourcePaths: List<String>,
        password: String?,
        compressionLevel: Int,
        onProgress: ((ArchiveProgress) -> Unit)?
    ): Result<Unit> {
        return Result.Failure(AppError.UnsupportedFormat("Tạo file 7z chưa được hỗ trợ. Vui lòng chọn định dạng ZIP."))
    }

    override suspend fun testIntegrity(archivePath: String, password: String?): Result<Boolean> = withContext(dispatchers.io) {
        try {
            val file = File(archivePath)
            if (!file.exists()) return@withContext Result.Failure(AppError.FileNotFound())
            val pwdBytes = password?.toByteArray()
            val sevenZFile = if (pwdBytes != null) SevenZFile.builder().setFile(file).setPassword(pwdBytes).get()
                             else SevenZFile.builder().setFile(file).get()
            sevenZFile.use { archive ->
                Result.Success(archive.entries.any())
            }
        } catch (e: Exception) {
            Result.Failure(AppError.CorruptedArchive("Kiểm tra toàn vẹn 7z thất bại: ${e.message}", e))
        }
    }
}
