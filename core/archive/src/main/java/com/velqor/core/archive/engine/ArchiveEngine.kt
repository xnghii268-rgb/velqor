package com.velqor.core.archive.engine

import com.velqor.core.archive.model.ArchiveEntry
import com.velqor.core.archive.model.ArchiveProgress
import com.velqor.core.common.result.Result
import java.io.File

interface ArchiveEngine {
    fun canHandle(archivePath: String): Boolean

    suspend fun listEntries(
        archivePath: String,
        password: String? = null
    ): Result<List<ArchiveEntry>>

    suspend fun extract(
        archivePath: String,
        destinationDir: String,
        selectedEntries: List<String>? = null,
        password: String? = null,
        onProgress: ((ArchiveProgress) -> Unit)? = null
    ): Result<Unit>

    suspend fun createArchive(
        archivePath: String,
        sourcePaths: List<String>,
        password: String? = null,
        compressionLevel: Int = 5,
        onProgress: ((ArchiveProgress) -> Unit)? = null
    ): Result<Unit>

    suspend fun testIntegrity(
        archivePath: String,
        password: String? = null
    ): Result<Boolean>

    /**
     * Zip Slip Defense: Validates destination file stays strictly within destination root.
     */
    fun validateDestinationPath(destinationDir: File, entryName: String): File {
        val destFile = File(destinationDir, entryName)
        val canonicalDestDirPath = destinationDir.canonicalPath
        val canonicalDestFilePath = destFile.canonicalPath

        if (!canonicalDestFilePath.startsWith(canonicalDestDirPath + File.separator) &&
            canonicalDestFilePath != canonicalDestDirPath
        ) {
            throw SecurityException("Cảnh báo Zip Slip: Entry '$entryName' cố tình thoát khỏi thư mục đích!")
        }
        return destFile
    }
}
