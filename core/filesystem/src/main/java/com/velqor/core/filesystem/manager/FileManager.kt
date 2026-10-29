package com.velqor.core.filesystem.manager

import com.velqor.core.common.dispatcher.DispatcherProvider
import com.velqor.core.common.result.AppError
import com.velqor.core.common.result.Result
import com.velqor.core.filesystem.model.FileItem
import com.velqor.core.filesystem.model.FileSortOption
import com.velqor.core.filesystem.model.SortField
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.Normalizer
import java.util.Locale
import java.util.regex.Pattern

class FileManager(
    private val dispatchers: DispatcherProvider
) {
    suspend fun listFiles(
        path: String,
        showHidden: Boolean = false,
        sort: FileSortOption = FileSortOption()
    ): Result<List<FileItem>> = withContext(dispatchers.io) {
        try {
            val directory = File(path)
            if (!directory.exists()) {
                return@withContext Result.Failure(AppError.FileNotFound("Đường dẫn không tồn tại: $path"))
            }
            if (!directory.isDirectory) {
                return@withContext Result.Failure(AppError.Unknown("Đường dẫn không phải là thư mục: $path"))
            }

            val rawFiles = directory.listFiles() ?: emptyArray()
            val filtered = rawFiles.filter { showHidden || !it.name.startsWith(".") }

            val items = filtered.map { file ->
                FileItem(
                    path = file.absolutePath,
                    name = file.name,
                    isDirectory = file.isDirectory,
                    sizeBytes = if (file.isDirectory) 0L else file.length(),
                    lastModifiedMs = file.lastModified(),
                    type = FileTypeResolver.resolve(file)
                )
            }

            val sorted = sortItems(items, sort)
            Result.Success(sorted)
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown("Lỗi duyệt thư mục: ${e.message}", e))
        }
    }

    private fun sortItems(items: List<FileItem>, sort: FileSortOption): List<FileItem> {
        val comparator = Comparator<FileItem> { a, b ->
            // Folders always first
            if (a.isDirectory && !b.isDirectory) return@Comparator -1
            if (!a.isDirectory && b.isDirectory) return@Comparator 1

            val comparison = when (sort.field) {
                SortField.NAME -> a.name.compareTo(b.name, ignoreCase = true)
                SortField.DATE -> a.lastModifiedMs.compareTo(b.lastModifiedMs)
                SortField.SIZE -> a.sizeBytes.compareTo(b.sizeBytes)
                SortField.TYPE -> a.type.name.compareTo(b.type.name)
            }
            if (sort.ascending) comparison else -comparison
        }
        return items.sortedWith(comparator)
    }

    suspend fun createDirectory(parentPath: String, name: String): Result<FileItem> = withContext(dispatchers.io) {
        try {
            val newDir = File(parentPath, name)
            if (newDir.exists()) {
                return@withContext Result.Failure(AppError.AlreadyExists("Thư mục '$name' đã tồn tại"))
            }
            if (newDir.mkdirs()) {
                Result.Success(
                    FileItem(
                        path = newDir.absolutePath,
                        name = newDir.name,
                        isDirectory = true,
                        sizeBytes = 0L,
                        lastModifiedMs = newDir.lastModified(),
                        type = FileTypeResolver.resolve(newDir)
                    )
                )
            } else {
                Result.Failure(AppError.Unknown("Không thể tạo thư mục '$name'"))
            }
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown("Lỗi tạo thư mục: ${e.message}", e))
        }
    }

    suspend fun createFile(parentPath: String, name: String): Result<FileItem> = withContext(dispatchers.io) {
        try {
            val newFile = File(parentPath, name)
            if (newFile.exists()) {
                return@withContext Result.Failure(AppError.AlreadyExists("Tệp '$name' đã tồn tại"))
            }
            if (newFile.createNewFile()) {
                Result.Success(
                    FileItem(
                        path = newFile.absolutePath,
                        name = newFile.name,
                        isDirectory = false,
                        sizeBytes = 0L,
                        lastModifiedMs = newFile.lastModified(),
                        type = FileTypeResolver.resolve(newFile)
                    )
                )
            } else {
                Result.Failure(AppError.Unknown("Không thể tạo tệp '$name'"))
            }
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown("Lỗi tạo tệp: ${e.message}", e))
        }
    }

    suspend fun rename(path: String, newName: String): Result<FileItem> = withContext(dispatchers.io) {
        try {
            val target = File(path)
            if (!target.exists()) {
                return@withContext Result.Failure(AppError.FileNotFound())
            }
            val destination = File(target.parentFile, newName)
            if (destination.exists()) {
                return@withContext Result.Failure(AppError.AlreadyExists("Tên '$newName' đã tồn tại"))
            }
            if (target.renameTo(destination)) {
                Result.Success(
                    FileItem(
                        path = destination.absolutePath,
                        name = destination.name,
                        isDirectory = destination.isDirectory,
                        sizeBytes = if (destination.isDirectory) 0L else destination.length(),
                        lastModifiedMs = destination.lastModified(),
                        type = FileTypeResolver.resolve(destination)
                    )
                )
            } else {
                Result.Failure(AppError.Unknown("Không thể đổi tên sang '$newName'"))
            }
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown("Lỗi đổi tên: ${e.message}", e))
        }
    }

    suspend fun delete(paths: List<String>): Result<Int> = withContext(dispatchers.io) {
        try {
            var count = 0
            for (p in paths) {
                val f = File(p)
                if (f.exists()) {
                    if (f.deleteRecursively()) {
                        count++
                    }
                }
            }
            Result.Success(count)
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown("Lỗi xóa tệp: ${e.message}", e))
        }
    }

    suspend fun copy(
        sourcePaths: List<String>,
        targetDirectory: String,
        onProgress: ((copiedBytes: Long, totalBytes: Long) -> Unit)? = null
    ): Result<Int> = withContext(dispatchers.io) {
        try {
            val destDir = File(targetDirectory)
            if (!destDir.exists()) destDir.mkdirs()

            // Calculate total bytes
            var totalBytes = 0L
            val sources = sourcePaths.map { File(it) }.filter { it.exists() }
            for (s in sources) {
                totalBytes += calculateSize(s)
            }

            var copiedBytes = 0L
            var copiedCount = 0

            for (src in sources) {
                val dest = File(destDir, src.name)
                copyRecursive(src, dest) { bytesChunk ->
                    copiedBytes += bytesChunk
                    onProgress?.invoke(copiedBytes, totalBytes)
                }
                copiedCount++
            }

            Result.Success(copiedCount)
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown("Lỗi sao chép: ${e.message}", e))
        }
    }

    suspend fun move(sourcePaths: List<String>, targetDirectory: String): Result<Int> = withContext(dispatchers.io) {
        try {
            val destDir = File(targetDirectory)
            if (!destDir.exists()) destDir.mkdirs()

            var movedCount = 0
            for (path in sourcePaths) {
                val src = File(path)
                if (!src.exists()) continue
                val dest = File(destDir, src.name)

                // Try fast atomic rename first
                if (src.renameTo(dest)) {
                    movedCount++
                } else {
                    // Fallback to copy + delete across mount points
                    copyRecursive(src, dest) {}
                    if (src.deleteRecursively()) {
                        movedCount++
                    }
                }
            }
            Result.Success(movedCount)
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown("Lỗi di chuyển: ${e.message}", e))
        }
    }

    fun search(rootPath: String, query: String, recursive: Boolean = true): Flow<FileItem> = flow {
        val root = File(rootPath)
        if (!root.exists() || !root.isDirectory) return@flow

        val normalizedQuery = removeAccents(query).lowercase(Locale.ROOT)

        fun searchDir(dir: File) {
            val files = dir.listFiles() ?: return
            for (file in files) {
                val nameNormalized = removeAccents(file.name).lowercase(Locale.ROOT)
                if (nameNormalized.contains(normalizedQuery)) {
                    val item = FileItem(
                        path = file.absolutePath,
                        name = file.name,
                        isDirectory = file.isDirectory,
                        sizeBytes = if (file.isDirectory) 0L else file.length(),
                        lastModifiedMs = file.lastModified(),
                        type = FileTypeResolver.resolve(file)
                    )
                    emit(item)
                }
                if (file.isDirectory && recursive) {
                    searchDir(file)
                }
            }
        }

        searchDir(root)
    }.flowOn(dispatchers.io)

    private fun calculateSize(file: File): Long {
        if (!file.isDirectory) return file.length()
        var size = 0L
        val children = file.listFiles() ?: return 0L
        for (c in children) {
            size += calculateSize(c)
        }
        return size
    }

    private fun copyRecursive(src: File, dest: File, onBytes: (Long) -> Unit) {
        if (src.isDirectory) {
            if (!dest.exists()) dest.mkdirs()
            val children = src.listFiles() ?: return
            for (c in children) {
                copyRecursive(c, File(dest, c.name), onBytes)
            }
        } else {
            FileInputStream(src).use { input ->
                FileOutputStream(dest).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } >= 0) {
                        output.write(buffer, 0, read)
                        onBytes(read.toLong())
                    }
                }
            }
        }
    }

    companion object {
        fun removeAccents(str: String): String {
            val nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD)
            val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
            return pattern.matcher(nfdNormalizedString)
                .replaceAll("")
                .replace("đ", "d")
                .replace("Đ", "D")
        }
    }
}
