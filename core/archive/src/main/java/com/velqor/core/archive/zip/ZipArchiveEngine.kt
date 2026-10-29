package com.velqor.core.archive.zip

import com.velqor.core.archive.engine.ArchiveEngine
import com.velqor.core.archive.model.ArchiveEntry
import com.velqor.core.archive.model.ArchiveProgress
import com.velqor.core.common.dispatcher.DispatcherProvider
import com.velqor.core.common.result.AppError
import com.velqor.core.common.result.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import net.lingala.zip4j.progress.ProgressMonitor
import java.io.File
import java.util.Locale

class ZipArchiveEngine(
    private val dispatchers: DispatcherProvider
) : ArchiveEngine {

    override fun canHandle(archivePath: String): Boolean {
        val ext = archivePath.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return ext in setOf("zip", "jar", "war", "ear", "apk")
    }

    override suspend fun listEntries(
        archivePath: String,
        password: String?
    ): Result<List<ArchiveEntry>> = withContext(dispatchers.io) {
        try {
            val file = File(archivePath)
            if (!file.exists()) return@withContext Result.Failure(AppError.FileNotFound())

            val zipFile = ZipFile(file)
            if (password != null) zipFile.setPassword(password.toCharArray())

            if (!zipFile.isValidZipFile) {
                return@withContext Result.Failure(AppError.CorruptedArchive("Tệp không phải định dạng ZIP hợp lệ"))
            }

            val headers = zipFile.fileHeaders ?: emptyList()
            val entries = headers.map { h ->
                ArchiveEntry(
                    path = h.fileName,
                    name = h.fileName.trimEnd('/').substringAfterLast('/'),
                    isDirectory = h.isDirectory,
                    uncompressedSize = h.uncompressedSize,
                    compressedSize = h.compressedSize,
                    isEncrypted = h.isEncrypted,
                    lastModifiedMs = h.lastModifiedTime
                )
            }
            Result.Success(entries)
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown("Lỗi đọc danh sách tệp ZIP: ${e.message}", e))
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

            val zipFile = ZipFile(file)
            if (password != null) zipFile.setPassword(password.toCharArray())

            if (zipFile.isEncrypted && password == null) {
                return@withContext Result.Failure(AppError.InvalidPassword("Tệp ZIP được mã hóa. Vui lòng nhập mật khẩu."))
            }

            // Verify Zip Slip for all headers before extracting
            val headers = zipFile.fileHeaders ?: emptyList()
            for (h in headers) {
                try {
                    validateDestinationPath(dest, h.fileName)
                } catch (se: SecurityException) {
                    return@withContext Result.Failure(AppError.ZipSlipAttackDetected(se.message ?: "Zip Slip"))
                }
            }

            zipFile.isRunInThread = true
            val monitor = zipFile.progressMonitor

            if (selectedEntries != null && selectedEntries.isNotEmpty()) {
                for (entryName in selectedEntries) {
                    val header = zipFile.getFileHeader(entryName)
                    if (header != null) {
                        zipFile.extractFile(header, dest.absolutePath)
                    }
                }
            } else {
                zipFile.extractAll(dest.absolutePath)
            }

            // Monitor extraction progress
            while (monitor.state == ProgressMonitor.State.BUSY) {
                onProgress?.invoke(
                    ArchiveProgress(
                        currentEntry = monitor.fileName ?: "",
                        bytesProcessed = monitor.workCompleted,
                        totalBytes = monitor.totalWork,
                        percent = (monitor.percentDone / 100f).coerceIn(0f, 1f)
                    )
                )
                delay(100)
            }

            if (monitor.result == ProgressMonitor.Result.ERROR) {
                return@withContext Result.Failure(
                    AppError.Unknown("Lỗi giải nén ZIP: ${monitor.exception?.message}", monitor.exception)
                )
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown("Lỗi giải nén ZIP: ${e.message}", e))
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
            val zipFile = File(archivePath)
            if (zipFile.exists()) zipFile.delete()

            val zip = ZipFile(zipFile)
            val parameters = ZipParameters().apply {
                compressionMethod = CompressionMethod.DEFLATE
                this.compressionLevel = when (compressionLevel) {
                    0 -> CompressionLevel.NO_COMPRESSION
                    in 1..3 -> CompressionLevel.FASTEST
                    in 4..6 -> CompressionLevel.NORMAL
                    in 7..8 -> CompressionLevel.MAXIMUM
                    else -> CompressionLevel.ULTRA
                }
                if (!password.isNullOrEmpty()) {
                    isEncryptFiles = true
                    encryptionMethod = EncryptionMethod.AES
                    aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                }
            }

            if (!password.isNullOrEmpty()) {
                zip.setPassword(password.toCharArray())
            }

            zip.isRunInThread = true
            val monitor = zip.progressMonitor

            for (path in sourcePaths) {
                val src = File(path)
                if (!src.exists()) continue
                if (src.isDirectory) {
                    zip.addFolder(src, parameters)
                } else {
                    zip.addFile(src, parameters)
                }
            }

            while (monitor.state == ProgressMonitor.State.BUSY) {
                onProgress?.invoke(
                    ArchiveProgress(
                        currentEntry = monitor.fileName ?: "",
                        bytesProcessed = monitor.workCompleted,
                        totalBytes = monitor.totalWork,
                        percent = (monitor.percentDone / 100f).coerceIn(0f, 1f)
                    )
                )
                delay(100)
            }

            if (monitor.result == ProgressMonitor.Result.ERROR) {
                return@withContext Result.Failure(
                    AppError.Unknown("Lỗi tạo tệp ZIP: ${monitor.exception?.message}", monitor.exception)
                )
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown("Lỗi tạo tệp ZIP: ${e.message}", e))
        }
    }

    override suspend fun testIntegrity(archivePath: String, password: String?): Result<Boolean> = withContext(dispatchers.io) {
        try {
            val file = File(archivePath)
            if (!file.exists()) return@withContext Result.Failure(AppError.FileNotFound())
            val zipFile = ZipFile(file)
            if (password != null) zipFile.setPassword(password.toCharArray())
            val isValid = zipFile.isValidZipFile
            Result.Success(isValid)
        } catch (e: Exception) {
            Result.Failure(AppError.CorruptedArchive("Kiểm tra toàn vẹn thất bại: ${e.message}", e))
        }
    }
}
