package com.velqor.feature.browser

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velqor.core.common.util.Formatters
import com.velqor.core.designsystem.component.VelqorEmptyState
import com.velqor.core.designsystem.component.VelqorLoadingIndicator
import com.velqor.core.designsystem.component.VelqorTopBar
import com.velqor.core.designsystem.theme.AppleBlue
import com.velqor.core.designsystem.theme.AppleGreen
import com.velqor.core.designsystem.theme.AppleOrange
import com.velqor.core.designsystem.theme.AppleRed
import com.velqor.core.designsystem.theme.LocalVelqorColors
import com.velqor.core.filesystem.model.FileItem
import com.velqor.core.filesystem.model.FileSortOption
import com.velqor.core.filesystem.model.FileType
import com.velqor.core.filesystem.model.SortField
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    onNavigateToSearch: (String) -> Unit,
    onOpenFile: (FileItem) -> Unit,
    onOpenArchive: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val colors = LocalVelqorColors.current

    var showMenu by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var showSortDialog by remember { mutableStateOf(false) }

    // Intercept back button to navigate parent folder
    BackHandler(enabled = state.currentPath != "/" && state.currentPath != "/sdcard") {
        viewModel.navigateUp()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // 1. Apple Top Bar
        val folderTitle = if (state.currentPath == "/sdcard" || state.currentPath == "/storage/emulated/0") {
            "Bộ nhớ trong"
        } else {
            File(state.currentPath).name.ifEmpty { "Gốc" }
        }

        VelqorTopBar(
            title = folderTitle,
            navigationIcon = if (state.currentPath != "/" && state.currentPath != "/sdcard") {
                {
                    IconButton(onClick = { viewModel.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = colors.textPrimary
                        )
                    }
                }
            } else null,
            actions = {
                IconButton(onClick = { onNavigateToSearch(state.currentPath) }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Tìm kiếm",
                        tint = colors.textPrimary
                    )
                }

                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Tùy chọn",
                        tint = colors.textPrimary
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Tạo thư mục mới") },
                        leadingIcon = { Icon(Icons.Default.Add, null) },
                        onClick = {
                            showMenu = false
                            newFolderName = ""
                            showNewFolderDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sắp xếp danh sách") },
                        leadingIcon = { Icon(Icons.Default.Sort, null) },
                        onClick = {
                            showMenu = false
                            showSortDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (state.showHidden) "Ẩn file ẩn" else "Hiện file ẩn") },
                        leadingIcon = {
                            Icon(if (state.showHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        },
                        onClick = {
                            showMenu = false
                            viewModel.toggleShowHidden()
                        }
                    )
                }
            }
        )

        // 2. Breadcrumb path info
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.surfaceSecondary.copy(alpha = 0.5f)
        ) {
            Text(
                text = state.currentPath,
                fontSize = 12.sp,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // 3. Multi-Select or Clipboard Action Bar
        AnimatedVisibility(visible = state.isMultiSelectMode || state.clipboardPaths.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.surfaceElevated,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.isMultiSelectMode) {
                        Text(
                            text = "Đã chọn ${state.selectedPaths.size} mục",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary
                        )

                        Row {
                            IconButton(onClick = { viewModel.copySelected() }) {
                                Icon(Icons.Default.ContentCopy, "Chép", tint = colors.textPrimary)
                            }
                            IconButton(onClick = { viewModel.cutSelected() }) {
                                Icon(Icons.Default.ContentCut, "Cắt", tint = colors.textPrimary)
                            }
                            IconButton(onClick = { viewModel.deleteSelected() }) {
                                Icon(Icons.Default.Delete, "Xóa", tint = AppleRed)
                            }
                        }
                    } else if (state.clipboardPaths.isNotEmpty()) {
                        Text(
                            text = "Đang giữ ${state.clipboardPaths.size} mục",
                            fontSize = 14.sp,
                            color = colors.textSecondary
                        )

                        TextButton(onClick = { viewModel.paste() }) {
                            Icon(Icons.Default.ContentPaste, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dán vào đây")
                        }
                    }
                }
            }
        }

        // 4. File Items List
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    VelqorLoadingIndicator()
                }
            } else if (state.items.isEmpty()) {
                VelqorEmptyState(
                    title = "Thư mục trống",
                    subtitle = "Không có tệp tin hoặc thư mục nào tại vị trí này."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.items, key = { it.path }) { item ->
                        val isSelected = state.selectedPaths.contains(item.path)

                        FileListItem(
                            item = item,
                            isSelected = isSelected,
                            isMultiSelectMode = state.isMultiSelectMode,
                            onClick = {
                                if (state.isMultiSelectMode) {
                                    viewModel.toggleItemSelection(item.path)
                                } else {
                                    if (item.isDirectory) {
                                        viewModel.loadDirectory(item.path)
                                    } else if (item.type.isArchive) {
                                        onOpenArchive(item.path)
                                    } else {
                                        onOpenFile(item)
                                    }
                                }
                            },
                            onLongClick = {
                                viewModel.toggleItemSelection(item.path)
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog: Tạo thư mục mới
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Tạo thư mục mới") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Tên thư mục") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createFolder(newFolderName.trim())
                            showNewFolderDialog = false
                        }
                    }
                ) {
                    Text("Tạo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog: Sắp xếp
    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("Sắp xếp theo") },
            text = {
                Column {
                    listOf(
                        SortField.NAME to "Tên (A-Z)",
                        SortField.DATE to "Ngày sửa đổi",
                        SortField.SIZE to "Kích thước",
                        SortField.TYPE to "Loại tệp tin"
                    ).forEach { (field, label) ->
                        TextButton(
                            onClick = {
                                viewModel.setSortOption(FileSortOption(field, ascending = true))
                                showSortDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSortDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    item: FileItem,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = LocalVelqorColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(if (isSelected) colors.accent.copy(alpha = 0.15f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isMultiSelectMode) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) colors.accent else colors.textTertiary,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 12.dp)
            )
        }

        // Icon based on type
        val (icon, iconColor) = resolveFileIcon(item)
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            val subtitle = if (item.isDirectory) {
                Formatters.formatDate(item.lastModifiedMs)
            } else {
                "${Formatters.formatFileSize(item.sizeBytes)} • ${Formatters.formatDate(item.lastModifiedMs)}"
            }

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = colors.textSecondary
            )
        }
    }
}

private fun resolveFileIcon(item: FileItem): Pair<ImageVector, Color> {
    return when (item.type) {
        FileType.FOLDER -> Icons.Default.Folder to AppleOrange
        FileType.ARCHIVE_ZIP, FileType.ARCHIVE_7Z, FileType.ARCHIVE_RAR, FileType.ARCHIVE_TAR ->
            Icons.Default.Folder to AppleBlue
        FileType.APK -> Icons.AutoMirrored.Filled.InsertDriveFile to AppleGreen
        else -> Icons.AutoMirrored.Filled.InsertDriveFile to Color.Gray
    }
}
