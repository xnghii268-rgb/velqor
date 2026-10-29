package com.velqor.core.filesystem

import com.velqor.core.common.dispatcher.DefaultDispatcherProvider
import com.velqor.core.filesystem.manager.FileManager
import com.velqor.core.filesystem.model.FileSortOption
import com.velqor.core.filesystem.model.SortField
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileManagerTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val fileManager = FileManager(DefaultDispatcherProvider())

    @Test
    fun testCreateAndListFiles() = runBlocking {
        val root = tempFolder.root.absolutePath
        val dirResult = fileManager.createDirectory(root, "test_folder")
        assertTrue(dirResult.isSuccess)

        val fileResult = fileManager.createFile(root, "sample.txt")
        assertTrue(fileResult.isSuccess)

        val listResult = fileManager.listFiles(root, showHidden = false, FileSortOption(SortField.NAME))
        assertTrue(listResult.isSuccess)

        val items = listResult.getOrNull()!!
        assertEquals(2, items.size)
        // Folder must be first
        assertTrue(items[0].isDirectory)
        assertEquals("test_folder", items[0].name)
        assertEquals("sample.txt", items[1].name)
    }

    @Test
    fun testSearchWithVietnameseAccents() = runBlocking {
        val root = tempFolder.root.absolutePath
        File(tempFolder.root, "Tài Liệu Quan Trọng.pdf").createNewFile()
        File(tempFolder.root, "tai_chinh.xlsx").createNewFile()
        File(tempFolder.root, "notes.txt").createNewFile()

        // Search "tai lieu" without accents should find "Tài Liệu Quan Trọng.pdf"
        val results = fileManager.search(root, "tai lieu").toList()
        assertEquals(1, results.size)
        assertEquals("Tài Liệu Quan Trọng.pdf", results[0].name)

        // Search "TAI" should match both
        val results2 = fileManager.search(root, "tai").toList()
        assertEquals(2, results2.size)
    }

    @Test
    fun testRenameAndDelete() = runBlocking {
        val root = tempFolder.root.absolutePath
        val file = File(tempFolder.root, "original.txt")
        file.writeText("Hello VELQOR")

        val renameResult = fileManager.rename(file.absolutePath, "renamed.txt")
        assertTrue(renameResult.isSuccess)

        val renamedFile = File(tempFolder.root, "renamed.txt")
        assertTrue(renamedFile.exists())

        val deleteResult = fileManager.delete(listOf(renamedFile.absolutePath))
        assertTrue(deleteResult.isSuccess)
        assertEquals(1, deleteResult.getOrNull())
        assertTrue(!renamedFile.exists())
    }
}
