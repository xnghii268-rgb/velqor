package com.velqor.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velqor.core.common.util.Formatters
import com.velqor.core.designsystem.component.VelqorEmptyState
import com.velqor.core.designsystem.component.VelqorTopBar
import com.velqor.core.designsystem.theme.AppleBlue
import com.velqor.core.designsystem.theme.AppleOrange
import com.velqor.core.designsystem.theme.LocalVelqorColors
import com.velqor.core.filesystem.manager.FileManager
import com.velqor.core.filesystem.model.FileItem
import com.velqor.core.filesystem.model.FileType
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect

@Composable
fun SearchScreen(
    fileManager: FileManager,
    searchRootPath: String,
    onNavigateBack: () -> Unit,
    onSelectFile: (FileItem) -> Unit
) {
    val colors = LocalVelqorColors.current
    var query by remember { mutableStateOf("") }
    val results = remember { mutableStateListOf<FileItem>() }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        results.clear()
        if (query.trim().length >= 2) {
            isSearching = true
            fileManager.search(searchRootPath, query.trim(), recursive = true)
                .catch { isSearching = false }
                .collect { item ->
                    results.add(item)
                }
            isSearching = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        VelqorTopBar(
            title = "Tìm kiếm",
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = colors.textPrimary
                    )
                }
            }
        )

        // Search Input Bar
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Tìm theo tên (hỗ trợ tiếng Việt không dấu)...") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = colors.textSecondary) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Clear, "Xóa", tint = colors.textSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = colors.surfaceElevated,
                unfocusedContainerColor = colors.surfaceElevated,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (query.length < 2) {
            VelqorEmptyState(
                title = "Nhập từ khóa tìm kiếm",
                subtitle = "Tìm kiếm tệp và thư mục bên trong '${searchRootPath.substringAfterLast('/')}'",
                icon = Icons.Default.Search
            )
        } else if (results.isEmpty() && !isSearching) {
            VelqorEmptyState(
                title = "Không tìm thấy kết quả",
                subtitle = "Không có tệp nào khớp với '$query'",
                icon = Icons.Default.Search
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(results, key = { it.path }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectFile(item) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = if (item.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile
                        val tint = if (item.isDirectory) AppleOrange else AppleBlue

                        Icon(icon, null, tint = tint, modifier = Modifier.size(32.dp))

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

                            Text(
                                text = item.path,
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
