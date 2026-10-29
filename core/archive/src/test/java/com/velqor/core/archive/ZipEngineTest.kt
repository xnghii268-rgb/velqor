package com.velqor.core.archive

import com.velqor.core.archive.zip.ZipArchiveEngine
import com.velqor.core.common.dispatcher.DefaultDispatcherProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ZipEngineTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val engine = ZipArchiveEngine(DefaultDispatcherProvider())

    @Test
    fun testZipCreateListAndExtract() = runBlocking {
        val root = tempFolder.root
        val file1 = File(root, "file1.txt")
        file1.writeText("Content 1")
        val file2 = File(root, "file2.txt")
        file2.writeText("Content 2")

        val zipFile = File(root, "output.zip")

        // 1. Create Archive
        val createResult = engine.createArchive(zipFile.absolutePath, listOf(file1.absolutePath, file2.absolutePath))
        assertTrue(createResult.isSuccess)
        assertTrue(zipFile.exists())

        // 2. List Entries
        val listResult = engine.listEntries(zipFile.absolutePath)
        assertTrue(listResult.isSuccess)
        val entries = listResult.getOrNull()!!
        assertEquals(2, entries.size)

        // 3. Extract Archive
        val extractDir = File(root, "extracted")
        val extractResult = engine.extract(zipFile.absolutePath, extractDir.absolutePath)
        assertTrue(extractResult.isSuccess)

        val extracted1 = File(extractDir, "file1.txt")
        assertTrue(extracted1.exists())
        assertEquals("Content 1", extracted1.readText())
    }

    @Test(expected = SecurityException::class)
    fun testZipSlipDefense() {
        val targetDir = File(tempFolder.root, "safe_dir")
        targetDir.mkdirs()

        // Malicious entry attempting path traversal outside safe_dir
        engine.validateDestinationPath(targetDir, "../../../evil_script.sh")
    }
}
