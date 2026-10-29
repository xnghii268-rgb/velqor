package com.velqor.feature.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velqor.core.filesystem.manager.FileManager
import com.velqor.core.filesystem.model.FileItem
import com.velqor.core.filesystem.model.FileSortOption
import com.velqor.core.filesystem.model.SortField
import com.velqor.core.tasks.model.TaskType
import com.velqor.core.tasks.queue.TaskManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class BrowserUiState(
    val currentPath: String = "/sdcard",
    val items: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showHidden: Boolean = false,
    val sortOption: FileSortOption = FileSortOption(SortField.NAME, ascending = true),
    val selectedPaths: Set<String> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val clipboardPaths: List<String> = emptyList(),
    val isCutMode: Boolean = false
)

class BrowserViewModel(
    private val fileManager: FileManager,
    private val taskManager: TaskManager,
    initialPath: String = "/sdcard"
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserUiState(currentPath = initialPath))
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    init {
        loadDirectory(initialPath)
    }

    fun loadDirectory(path: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, currentPath = path, errorMessage = null) }
            val result = fileManager.listFiles(
                path = path,
                showHidden = _uiState.value.showHidden,
                sort = _uiState.value.sortOption
            )
            result.onSuccess { list ->
                _uiState.update { it.copy(items = list, isLoading = false, selectedPaths = emptySet(), isMultiSelectMode = false) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
            }
        }
    }

    fun navigateUp(): Boolean {
        val current = File(_uiState.value.currentPath)
        val parent = current.parentFile
        return if (parent != null && parent.canRead()) {
            loadDirectory(parent.absolutePath)
            true
        } else {
            false
        }
    }

    fun toggleItemSelection(path: String) {
        _uiState.update { state ->
            val newSelected = state.selectedPaths.toMutableSet()
            if (newSelected.contains(path)) {
                newSelected.remove(path)
            } else {
                newSelected.add(path)
            }
            state.copy(
                selectedPaths = newSelected,
                isMultiSelectMode = newSelected.isNotEmpty()
            )
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            val allPaths = state.items.map { it.path }.toSet()
            state.copy(selectedPaths = allPaths, isMultiSelectMode = true)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedPaths = emptySet(), isMultiSelectMode = false) }
    }

    fun setSortOption(sortOption: FileSortOption) {
        _uiState.update { it.copy(sortOption = sortOption) }
        loadDirectory(_uiState.value.currentPath)
    }

    fun toggleShowHidden() {
        _uiState.update { it.copy(showHidden = !it.showHidden) }
        loadDirectory(_uiState.value.currentPath)
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            fileManager.createDirectory(_uiState.value.currentPath, name)
            loadDirectory(_uiState.value.currentPath)
        }
    }

    fun createFile(name: String) {
        viewModelScope.launch {
            fileManager.createFile(_uiState.value.currentPath, name)
            loadDirectory(_uiState.value.currentPath)
        }
    }

    fun renameItem(path: String, newName: String) {
        viewModelScope.launch {
            fileManager.rename(path, newName)
            loadDirectory(_uiState.value.currentPath)
        }
    }

    fun deleteSelected() {
        val targets = _uiState.value.selectedPaths.toList()
        if (targets.isEmpty()) return
        taskManager.enqueueTask("Xóa ${targets.size} mục", TaskType.DELETE) { _, _ ->
            fileManager.delete(targets)
            loadDirectory(_uiState.value.currentPath)
        }
        clearSelection()
    }

    fun copySelected() {
        _uiState.update { it.copy(clipboardPaths = it.selectedPaths.toList(), isCutMode = false) }
        clearSelection()
    }

    fun cutSelected() {
        _uiState.update { it.copy(clipboardPaths = it.selectedPaths.toList(), isCutMode = true) }
        clearSelection()
    }

    fun paste() {
        val sources = _uiState.value.clipboardPaths
        val dest = _uiState.value.currentPath
        val isCut = _uiState.value.isCutMode
        if (sources.isEmpty()) return

        val title = if (isCut) "Di chuyển ${sources.size} mục" else "Sao chép ${sources.size} mục"
        val type = if (isCut) TaskType.MOVE else TaskType.COPY

        taskManager.enqueueTask(title, type) { updateProgress, _ ->
            if (isCut) {
                fileManager.move(sources, dest)
            } else {
                fileManager.copy(sources, dest) { copied, total ->
                    val pct = if (total > 0) copied.toFloat() / total else 0f
                    updateProgress("Đang chép...", pct, copied, total)
                }
            }
            loadDirectory(dest)
        }
        _uiState.update { it.copy(clipboardPaths = emptyList()) }
    }
}
