package com.velqor.core.security.hash

import com.velqor.core.common.dispatcher.DispatcherProvider
import com.velqor.core.common.result.AppError
import com.velqor.core.common.result.Result
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Locale

class Sha256Hasher(
    private val dispatchers: DispatcherProvider
) {
    suspend fun calculateHash(
        filePath: String,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null
    ): Result<String> = withContext(dispatchers.io) {
        try {
            val file = File(filePath)
            if (!file.exists()) return@withContext Result.Failure(AppError.FileNotFound())

            val digest = MessageDigest.getInstance("SHA-256")
            val totalBytes = file.length()
            var bytesReadTotal = 0L

            FileInputStream(file).use { input ->
                val buffer = ByteArray(64 * 1024)
                var read: Int
                while (input.read(buffer).also { read = it } >= 0) {
                    digest.update(buffer, 0, read)
                    bytesReadTotal += read
                    onProgress?.invoke(bytesReadTotal, totalBytes)
                }
            }

            val hashBytes = digest.digest()
            val hexString = hashBytes.joinToString("") { "%02x".format(it) }
            Result.Success(hexString)
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown("Lỗi tính mã SHA-256: ${e.message}", e))
        }
    }

    fun verifyHash(actualHash: String, expectedHash: String): Boolean {
        return actualHash.trim().equals(expectedHash.trim(), ignoreCase = true)
    }
}
