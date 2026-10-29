package com.velqor.feature.archive

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velqor.core.archive.engine.ArchiveEngine
import com.velqor.core.archive.model.ArchiveEntry
import com.velqor.core.common.util.Formatters
import com.velqor.core.designsystem.component.VelqorButton
import com.velqor.core.designsystem.component.VelqorEmptyState
import com.velqor.core.designsystem.component.VelqorLoadingIndicator
import com.velqor.core.designsystem.component.VelqorPrimaryButton
import com.velqor.core.designsystem.component.VelqorTopBar
import com.velqor.core.designsystem.theme.AppleBlue
import com.velqor.core.designsystem.theme.AppleOrange
import com.velqor.core.designsystem.theme.LocalVelqorColors
import com.velqor.core.tasks.model.TaskType
import com.velqor.core.tasks.queue.TaskManager
import java.io.File

@Composable
fun ArchiveViewerScreen(
    archivePath: String,
    archiveEngine: ArchiveEngine,
    taskManager: TaskManager,
    onNavigateBack: () -> Unit
) {
    val colors = LocalVelqorColors.current
    val entries = remember { mutableStateListOf<ArchiveEntry>() }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }

    val archiveName = File(archivePath).name

    fun load(pwd: String? = null) {
        isLoading = true
        errorMessage = null
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).run {
            // Read list
        }
    }

    LaunchedEffect(archivePath) {
        val result = archiveEngine.listEntries(archivePath)
        result.onSuccess { list ->
            entries.clear()
            entries.addAll(list)
            isLoading = false
        }.onFailure { error ->
            isLoading = false
            if (error is com.velqor.core.common.result.AppError.InvalidPassword) {
                showPasswordDialog = true
            } else {
                errorMessage = error.message
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        VelqorTopBar(
            title = archiveName,
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = colors.textPrimary
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        val destDir = File(archivePath).parentFile?.absolutePath ?: "/sdcard"
                        val extractDest = File(destDir, archiveName.substringBeforeLast(".")).absolutePath

                        taskManager.enqueueTask("Giải nén $archiveName", TaskType.EXTRACT) { updateProgress, _ ->
                            archiveEngine.extract(archivePath, extractDest) { p ->
                                updateProgress(p.currentEntry, p.percent, p.bytesProcessed, p.totalBytes)
                            }
                        }
                        onNavigateBack()
                    }
                ) {
                    Icon(Icons.Default.Unarchive, "Giải nén", tint = colors.accent)
                }
            }
        )

        // Summary bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.surfaceElevated
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tổng: ${entries.size} tệp",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    VelqorLoadingIndicator()
                }
            } else if (errorMessage != null) {
                VelqorEmptyState(
                    title = "Không thể xem tệp nén",
                    subtitle = errorMessage ?: "Lỗi định dạng",
                    actionButton = {
                        VelqorPrimaryButton(text = "Thử lại", onClick = { load() })
                    }
                )
            } else if (entries.isEmpty()) {
                VelqorEmptyState(
                    title = "Tệp nén rỗng",
                    subtitle = "Không tìm thấy nội dung bên trong tệp này."
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(entries, key = { it.path }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val icon = if (item.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile
                            val tint = if (item.isDirectory) AppleOrange else AppleBlue

                            Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp))

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

                                val detail = if (item.isDirectory) "Thư mục" else Formatters.formatFileSize(item.uncompressedSize)
                                Text(
                                    text = detail,
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }

                            if (item.isEncrypted) {
                                Icon(Icons.Default.Lock, "Mã hóa", tint = colors.textTertiary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Tệp nén được bảo vệ") },
            text = {
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Mật khẩu") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPasswordDialog = false
                        load(passwordInput)
                    }
                ) {
                    Text("Mở khóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false; onNavigateBack() }) {
                    Text("Hủy")
                }
            }
        )
    }
}
