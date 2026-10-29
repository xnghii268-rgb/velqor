package com.velqor.app.di

import android.content.Context
import com.velqor.core.archive.engine.ArchiveEngine
import com.velqor.core.archive.engine.CompositeArchiveEngine
import com.velqor.core.common.dispatcher.DefaultDispatcherProvider
import com.velqor.core.common.dispatcher.DispatcherProvider
import com.velqor.core.filesystem.manager.FileManager
import com.velqor.core.security.hash.Sha256Hasher
import com.velqor.core.security.vault.VaultCipher
import com.velqor.core.tasks.queue.TaskManager
import java.io.File

class AppContainer(private val context: Context) {
    val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
    val fileManager: FileManager = FileManager(dispatchers)
    val taskManager: TaskManager = TaskManager(dispatchers)
    val archiveEngine: ArchiveEngine = CompositeArchiveEngine(dispatchers)
    val vaultCipher: VaultCipher = VaultCipher(dispatchers)
    val sha256Hasher: Sha256Hasher = Sha256Hasher(dispatchers)

    val vaultDirectory: File
        get() = File(context.filesDir, "vault")
}
