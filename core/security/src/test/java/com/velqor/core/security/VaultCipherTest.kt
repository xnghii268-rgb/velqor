package com.velqor.core.security

import com.velqor.core.common.dispatcher.DefaultDispatcherProvider
import com.velqor.core.security.hash.Sha256Hasher
import com.velqor.core.security.vault.VaultCipher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile

class VaultCipherTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val cipher = VaultCipher(DefaultDispatcherProvider())
    private val hasher = Sha256Hasher(DefaultDispatcherProvider())

    @Test
    fun testEncryptAndDecryptMatches() = runBlocking {
        val root = tempFolder.root
        val originalFile = File(root, "confidential.txt")
        val originalContent = "This is a highly confidential document in VELQOR Vault."
        originalFile.writeText(originalContent)

        val encryptedFile = File(root, "confidential.vqr")
        val decryptedFile = File(root, "restored.txt")

        // 1. Encrypt
        val encryptResult = cipher.encryptFile(originalFile, encryptedFile, "SecretPassword123")
        assertTrue(encryptResult.isSuccess)
        assertTrue(encryptedFile.exists())
        assertTrue(encryptedFile.length() > originalFile.length())

        // 2. Decrypt
        val decryptResult = cipher.decryptFile(encryptedFile, decryptedFile, "SecretPassword123")
        assertTrue(decryptResult.isSuccess)
        assertTrue(decryptedFile.exists())
        assertEquals(originalContent, decryptedFile.readText())
    }

    @Test
    fun testTamperedEncryptedFileFailsDecryption() = runBlocking {
        val root = tempFolder.root
        val originalFile = File(root, "plain.txt")
        originalFile.writeText("Critical Data")

        val encryptedFile = File(root, "tampered.vqr")
        val decryptedFile = File(root, "output.txt")

        cipher.encryptFile(originalFile, encryptedFile, "Key999")

        // Tamper with 1 byte in the ciphertext payload
        RandomAccessFile(encryptedFile, "rw").use { raf ->
            raf.seek(encryptedFile.length() - 5)
            val b = raf.readByte()
            raf.seek(encryptedFile.length() - 5)
            raf.writeByte(b.toInt() xor 0xFF)
        }

        // Decryption MUST fail due to AES-GCM tag mismatch
        val decryptResult = cipher.decryptFile(encryptedFile, decryptedFile, "Key999")
        assertTrue(decryptResult.isFailure)
        assertFalse(decryptedFile.exists())
    }

    @Test
    fun testSha256Hasher() = runBlocking {
        val file = File(tempFolder.root, "hash_test.txt")
        file.writeText("hello world")

        val result = hasher.calculateHash(file.absolutePath)
        assertTrue(result.isSuccess)
        val hash = result.getOrNull()!!
        // Known SHA-256 for "hello world" (no newline): b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9
        assertEquals("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9", hash)
        assertTrue(hasher.verifyHash(hash, "B94D27B9934D3E08A52E52D7DA7DABFAC484EFE37A5380EE9088F7ACE2EFCDE9"))
    }
}
