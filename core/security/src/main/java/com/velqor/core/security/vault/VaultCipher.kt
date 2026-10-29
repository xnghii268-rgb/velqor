package com.velqor.core.security.vault

import com.velqor.core.common.dispatcher.DispatcherProvider
import com.velqor.core.common.result.AppError
import com.velqor.core.common.result.Result
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class VaultCipher(
    private val dispatchers: DispatcherProvider
) {
    companion object {
        private val MAGIC_HEADER = "VELQOR_V1".toByteArray(Charsets.US_ASCII)
        private const val SALT_SIZE = 16
        private const val GCM_IV_SIZE = 12
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val CHUNK_SIZE = 64 * 1024 // 64 KB
        private const val PBKDF2_ITERATIONS = 65536
        private const val KEY_LENGTH_BITS = 256
    }

    private val secureRandom = SecureRandom()

    fun deriveKeyFromPassword(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    suspend fun encryptFile(
        sourceFile: File,
        targetEncryptedFile: File,
        password: String,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null
    ): Result<Unit> = withContext(dispatchers.io) {
        try {
            if (!sourceFile.exists()) return@withContext Result.Failure(AppError.FileNotFound())

            val salt = ByteArray(SALT_SIZE).apply { secureRandom.nextBytes(this) }
            val key = deriveKeyFromPassword(password, salt)
            val totalBytes = sourceFile.length()
            var bytesReadTotal = 0L

            targetEncryptedFile.parentFile?.mkdirs()

            FileInputStream(sourceFile).use { input ->
                FileOutputStream(targetEncryptedFile).use { output ->
                    // 1. Write Header: Magic + Salt + Chunk Size
                    output.write(MAGIC_HEADER)
                    output.write(salt)
                    output.write(ByteBuffer.allocate(4).putInt(CHUNK_SIZE).array())

                    val buffer = ByteArray(CHUNK_SIZE)
                    var chunkIndex = 0L
                    var read: Int

                    while (input.read(buffer).also { read = it } > 0) {
                        // 2. Generate unique 12-byte IV for this chunk
                        val iv = ByteArray(GCM_IV_SIZE).apply { secureRandom.nextBytes(this) }
                        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
                        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

                        // Authenticate chunk index sequence into AAD
                        cipher.updateAAD(ByteBuffer.allocate(8).putLong(chunkIndex).array())

                        val ciphertext = cipher.doFinal(buffer, 0, read)

                        // 3. Write chunk header: IV + ciphertext length + ciphertext
                        output.write(iv)
                        output.write(ByteBuffer.allocate(4).putInt(ciphertext.size).array())
                        output.write(ciphertext)

                        bytesReadTotal += read
                        chunkIndex++
                        onProgress?.invoke(bytesReadTotal, totalBytes)
                    }
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            targetEncryptedFile.delete()
            Result.Failure(AppError.CryptoFailed("Mã hóa tệp thất bại: ${e.message}", e))
        }
    }

    suspend fun decryptFile(
        encryptedFile: File,
        targetDecryptedFile: File,
        password: String,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null
    ): Result<Unit> = withContext(dispatchers.io) {
        try {
            if (!encryptedFile.exists()) return@withContext Result.Failure(AppError.FileNotFound())

            val totalBytes = encryptedFile.length()
            var bytesReadTotal = 0L

            targetDecryptedFile.parentFile?.mkdirs()

            FileInputStream(encryptedFile).use { input ->
                FileOutputStream(targetDecryptedFile).use { output ->
                    // 1. Read Header
                    val magic = ByteArray(MAGIC_HEADER.size)
                    if (input.read(magic) != MAGIC_HEADER.size || !magic.contentEquals(MAGIC_HEADER)) {
                        return@withContext Result.Failure(AppError.CorruptedArchive("Tệp không đúng định dạng mã hóa VELQOR"))
                    }

                    val salt = ByteArray(SALT_SIZE)
                    if (input.read(salt) != SALT_SIZE) {
                        return@withContext Result.Failure(AppError.CorruptedArchive("Dữ liệu salt bị thiếu hoặc hỏng"))
                    }

                    val chunkSizeBuf = ByteArray(4)
                    if (input.read(chunkSizeBuf) != 4) {
                        return@withContext Result.Failure(AppError.CorruptedArchive("Header tệp bị hỏng"))
                    }

                    val key = deriveKeyFromPassword(password, salt)
                    bytesReadTotal += MAGIC_HEADER.size + SALT_SIZE + 4

                    var chunkIndex = 0L
                    val iv = ByteArray(GCM_IV_SIZE)
                    val lenBuf = ByteArray(4)

                    while (input.read(iv) == GCM_IV_SIZE) {
                        if (input.read(lenBuf) != 4) {
                            return@withContext Result.Failure(AppError.CryptoFailed("Dữ liệu khối mã hóa bị ngắt quãng"))
                        }
                        val cipherLength = ByteBuffer.wrap(lenBuf).int
                        if (cipherLength <= 0 || cipherLength > CHUNK_SIZE + 1024) {
                            return@withContext Result.Failure(AppError.CryptoFailed("Kích thước khối dữ liệu không hợp lệ"))
                        }

                        val cipherBytes = ByteArray(cipherLength)
                        var readSoFar = 0
                        while (readSoFar < cipherLength) {
                            val r = input.read(cipherBytes, readSoFar, cipherLength - readSoFar)
                            if (r < 0) break
                            readSoFar += r
                        }
                        if (readSoFar != cipherLength) {
                            return@withContext Result.Failure(AppError.CryptoFailed("Không thể đọc đủ dữ liệu khối mã hóa"))
                        }

                        // Decrypt chunk with GCM authentication
                        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
                        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
                        cipher.updateAAD(ByteBuffer.allocate(8).putLong(chunkIndex).array())

                        val plaintext = cipher.doFinal(cipherBytes)
                        output.write(plaintext)

                        bytesReadTotal += GCM_IV_SIZE + 4 + cipherLength
                        chunkIndex++
                        onProgress?.invoke(bytesReadTotal, totalBytes)
                    }
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            targetDecryptedFile.delete()
            Result.Failure(AppError.CryptoFailed("Giải mã thất bại (mật khẩu sai hoặc tệp bị chỉnh sửa)", e))
        }
    }
}
