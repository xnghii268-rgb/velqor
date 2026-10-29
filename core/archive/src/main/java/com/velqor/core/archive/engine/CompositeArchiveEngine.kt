package com.velqor.core.archive.engine

import com.velqor.core.archive.model.ArchiveEntry
import com.velqor.core.archive.model.ArchiveProgress
import com.velqor.core.archive.rar.RarArchiveEngine
import com.velqor.core.archive.sevenz.SevenZipArchiveEngine
import com.velqor.core.archive.tar.TarArchiveEngine
import com.velqor.core.archive.zip.ZipArchiveEngine
import com.velqor.core.common.dispatcher.DispatcherProvider
import com.velqor.core.common.result.AppError
import com.velqor.core.common.result.Result

class CompositeArchiveEngine(
    dispatchers: DispatcherProvider
) : ArchiveEngine {

    private val engines = listOf(
        ZipArchiveEngine(dispatchers),
        SevenZipArchiveEngine(dispatchers),
        RarArchiveEngine(dispatchers),
        TarArchiveEngine(dispatchers)
    )

    private fun findEngine(archivePath: String): ArchiveEngine? {
        return engines.firstOrNull { it.canHandle(archivePath) }
    }

    override fun canHandle(archivePath: String): Boolean {
        return findEngine(archivePath) != null
    }

    override suspend fun listEntries(archivePath: String, password: String?): Result<List<ArchiveEntry>> {
        val engine = findEngine(archivePath)
            ?: return Result.Failure(AppError.UnsupportedFormat("Định dạng tệp nén không được hỗ trợ"))
        return engine.listEntries(archivePath, password)
    }

    override suspend fun extract(
        archivePath: String,
        destinationDir: String,
        selectedEntries: List<String>?,
        password: String?,
        onProgress: ((ArchiveProgress) -> Unit)?
    ): Result<Unit> {
        val engine = findEngine(archivePath)
            ?: return Result.Failure(AppError.UnsupportedFormat("Định dạng tệp nén không được hỗ trợ"))
        return engine.extract(archivePath, destinationDir, selectedEntries, password, onProgress)
    }

    override suspend fun createArchive(
        archivePath: String,
        sourcePaths: List<String>,
        password: String?,
        compressionLevel: Int,
        onProgress: ((ArchiveProgress) -> Unit)?
    ): Result<Unit> {
        val engine = findEngine(archivePath)
            ?: return Result.Failure(AppError.UnsupportedFormat("Định dạng tệp nén không được hỗ trợ để tạo"))
        return engine.createArchive(archivePath, sourcePaths, password, compressionLevel, onProgress)
    }

    override suspend fun testIntegrity(archivePath: String, password: String?): Result<Boolean> {
        val engine = findEngine(archivePath)
            ?: return Result.Failure(AppError.UnsupportedFormat("Định dạng tệp nén không được hỗ trợ"))
        return engine.testIntegrity(archivePath, password)
    }
}
