package com.velqor.core.archive.rar

import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import com.velqor.core.archive.engine.ArchiveEngine
import com.velqor.core.archive.model.ArchiveEntry
import com.velqor.core.archive.model.ArchiveProgress
import com.velqor.core.common.dispatcher.DispatcherProvider
import com.velqor.core.common.result.AppError
import com.velqor.core.common.result.Result
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class RarArchiveEngine(
    private val dispatchers: DispatcherProvider
) : ArchiveEngine {

    override fun canHandle(archivePath: String): Boolean {
        return archivePath.endsWith(".rar", ignoreCase = true)
    }

    override suspend fun listEntries(archivePath: String, password: String?): Result<List<ArchiveEntry>> = withContext(dispatchers.io) {
        try {
            val file = File(archivePath)
            if (!file.exists()) return@withContext Result.Failure(AppError.FileNotFound())

            val archive = Archive(file, password)
            archive.use { rar ->
                if (rar.isEncrypted && password.isNullOrEmpty()) {
                    return@withContext Result.Failure(AppError.InvalidPassword("Tệp RAR được mã hóa. Vui lòng nhập mật khẩu."))
                }

                val list = mutableListOf<ArchiveEntry>()
                for (header in rar.fileHeaders) {
                    val name = header.fileName.replace('\\', '/')
                    list.add(
                        ArchiveEntry(
                            path = name,
                            name = name.trimEnd('/').substringAfterLast('/'),
                            isDirectory = header.isDirectory,
                            uncompressedSize = header.unpSize,
                            compressedSize = header.packSize,
                            isEncrypted = header.isEncrypted,
                            lastModifiedMs = header.mTime?.time ?: 0L
                        )
                    )
                }
                Result.Success(list)
            }
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown("Lỗi đọc tệp RAR: ${e.message}", e))
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

            val archive = Archive(file, password)
            archive.use { rar ->
                val headers = rar.fileHeaders
                val total = headers.size
                var processed = 0
                val selectedSet = selectedEntries?.toSet()

                for (header in headers) {
                    val normalizedName = header.fileName.replace('\\', '/')
                    processed++

                    if (selectedSet != null && !selectedSet.contains(normalizedName)) {
                        continue
                    }

                    val destFile = try {
                        validateDestinationPath(dest, normalizedName)
                    } catch (se: SecurityException) {
                        return@withContext Result.Failure(AppError.ZipSlipAttackDetected(se.message ?: "Zip Slip"))
                    }

                    if (header.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        FileOutputStream(destFile).use { out ->
                            rar.extractFile(header, out)
                        }
                    }

                    onProgress?.invoke(
                        ArchiveProgress(
                            currentEntry = normalizedName,
                            entriesProcessed = processed,
                            totalEntries = total,
                            percent = (processed.toFloat() / total).coerceIn(0f, 1f)
                        )
                    )
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown("Lỗi giải nén RAR: ${e.message}", e))
        }
    }

    override suspend fun createArchive(
        archivePath: String,
        sourcePaths: List<String>,
        password: String?,
        compressionLevel: Int,
        onProgress: ((ArchiveProgress) -> Unit)?
    ): Result<Unit> {
        return Result.Failure(AppError.UnsupportedFormat("Định dạng RAR là chuẩn đóng độc quyền. Vui lòng sử dụng ZIP để nén tệp."))
    }

    override suspend fun testIntegrity(archivePath: String, password: String?): Result<Boolean> = withContext(dispatchers.io) {
        try {
            val file = File(archivePath)
            if (!file.exists()) return@withContext Result.Failure(AppError.FileNotFound())
            val archive = Archive(file, password)
            archive.use { rar ->
                Result.Success(rar.fileHeaders.isNotEmpty())
            }
        } catch (e: Exception) {
            Result.Failure(AppError.CorruptedArchive("Kiểm tra toàn vẹn RAR thất bại: ${e.message}", e))
        }
    }
}
