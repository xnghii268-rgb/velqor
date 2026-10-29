package com.velqor.core.archive.model

data class ArchiveEntry(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val uncompressedSize: Long,
    val compressedSize: Long,
    val isEncrypted: Boolean = false,
    val lastModifiedMs: Long = 0L
)

data class ArchiveProgress(
    val currentEntry: String = "",
    val entriesProcessed: Int = 0,
    val totalEntries: Int = 0,
    val bytesProcessed: Long = 0L,
    val totalBytes: Long = 0L,
    val percent: Float = 0f
)
