package com.velqor.core.filesystem.manager

import com.velqor.core.filesystem.model.FileType
import java.io.File
import java.util.Locale

object FileTypeResolver {
    fun resolve(file: File): FileType {
        if (file.isDirectory) return FileType.FOLDER

        val ext = file.extension.lowercase(Locale.ROOT)
        return when (ext) {
            "zip", "jar", "war", "ear" -> FileType.ARCHIVE_ZIP
            "7z" -> FileType.ARCHIVE_7Z
            "rar" -> FileType.ARCHIVE_RAR
            "tar", "gz", "tgz", "bz2", "tbz2", "xz", "txz" -> FileType.ARCHIVE_TAR
            "apk", "xapk", "apks" -> FileType.APK
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md" -> FileType.DOCUMENT
            "png", "jpg", "jpeg", "webp", "gif", "svg", "bmp", "heic" -> FileType.IMAGE
            "mp4", "mkv", "mov", "avi", "webm", "flv", "wmv" -> FileType.VIDEO
            "mp3", "m4a", "flac", "wav", "aac", "ogg", "opus" -> FileType.AUDIO
            "kt", "java", "py", "c", "cpp", "h", "cs", "js", "ts", "json", "xml", "html", "css", "sh" -> FileType.CODE
            else -> FileType.UNKNOWN
        }
    }
}
