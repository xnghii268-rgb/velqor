package com.velqor.feature.vault

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velqor.core.common.util.Formatters
import com.velqor.core.designsystem.component.VelqorCard
import com.velqor.core.designsystem.component.VelqorEmptyState
import com.velqor.core.designsystem.component.VelqorPrimaryButton
import com.velqor.core.designsystem.component.VelqorTopBar
import com.velqor.core.designsystem.theme.AppleBlue
import com.velqor.core.designsystem.theme.AppleGreen
import com.velqor.core.designsystem.theme.LocalVelqorColors
import com.velqor.core.security.vault.VaultCipher
import com.velqor.core.tasks.model.TaskType
import com.velqor.core.tasks.queue.TaskManager
import java.io.File

@Composable
fun VaultScreen(
    vaultDirectory: File,
    vaultCipher: VaultCipher,
    taskManager: TaskManager
) {
    val colors = LocalVelqorColors.current
    val vaultFiles = remember { mutableStateListOf<File>() }

    var showDecryptDialog by remember { mutableStateOf<File?>(null) }
    var passwordInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        if (!vaultDirectory.exists()) vaultDirectory.mkdirs()
        vaultFiles.clear()
        val list = vaultDirectory.listFiles()?.filter { it.isFile } ?: emptyList()
        vaultFiles.addAll(list)
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            VelqorTopBar(title = "Kho Bảo Mật (AES-256)")

            // Header Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = colors.surfaceElevated,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = AppleGreen,
                        modifier = Modifier.size(36.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Mã hóa cấp quân sự",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tệp được bảo vệ bằng AES-256-GCM chunked stream, chống trích xuất dữ liệu trái phép.",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            if (vaultFiles.isEmpty()) {
                VelqorEmptyState(
                    title = "Kho bảo mật trống",
                    subtitle = "Chưa có tệp nào được mã hóa. Chọn tệp từ trình quản lý để đưa vào kho.",
                    icon = Icons.Default.Lock
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(vaultFiles, key = { it.absolutePath }) { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, null, tint = AppleBlue, modifier = Modifier.size(28.dp))

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "${Formatters.formatFileSize(file.length())} • Đã mã hóa",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }

                            IconButton(onClick = {
                                passwordInput = ""
                                errorMessage = null
                                showDecryptDialog = file
                            }) {
                                Icon(Icons.Default.LockOpen, "Giải mã", tint = colors.accent)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDecryptDialog != null) {
        val targetFile = showDecryptDialog!!
        AlertDialog(
            onDismissRequest = { showDecryptDialog = null },
            title = { Text("Giải mã tệp") },
            text = {
                Column {
                    Text("Nhập mật khẩu để giải mã '${targetFile.name}':", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Mật khẩu") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = errorMessage != null
                    )
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(errorMessage!!, color = Color.Red, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val outDir = File("/sdcard/Download")
                        val outFile = File(outDir, targetFile.name.removeSuffix(".vqr"))

                        taskManager.enqueueTask("Giải mã ${targetFile.name}", TaskType.DECRYPT) { updateProgress, _ ->
                            val result = vaultCipher.decryptFile(targetFile, outFile, passwordInput) { cur, tot ->
                                val pct = if (tot > 0) cur.toFloat() / tot else 0f
                                updateProgress("Đang giải mã...", pct, cur, tot)
                            }
                            if (result.isFailure) {
                                errorMessage = result.errorOrNull()?.message
                            } else {
                                showDecryptDialog = null
                                refresh()
                            }
                        }
                    }
                ) {
                    Text("Giải mã")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDecryptDialog = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}
