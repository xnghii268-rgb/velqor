package com.velqor.core.filesystem.model

enum class FileType {
    FOLDER,
    ARCHIVE_ZIP,
    ARCHIVE_7Z,
    ARCHIVE_RAR,
    ARCHIVE_TAR,
    DOCUMENT,
    IMAGE,
    VIDEO,
    AUDIO,
    CODE,
    APK,
    UNKNOWN;

    val isArchive: Boolean
        get() = this in setOf(ARCHIVE_ZIP, ARCHIVE_7Z, ARCHIVE_RAR, ARCHIVE_TAR)
}

enum class SortField {
    NAME,
    DATE,
    SIZE,
    TYPE
}

data class FileSortOption(
    val field: SortField = SortField.NAME,
    val ascending: Boolean = true
)

data class FileItem(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModifiedMs: Long,
    val type: FileType,
    val isHidden: Boolean = name.startsWith("."),
    val isSelected: Boolean = false
) {
    val extension: String
        get() = if (isDirectory) "" else name.substringAfterLast('.', "")
}
