package com.velqor.feature.browser

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velqor.core.common.dispatcher.DefaultDispatcherProvider
import com.velqor.core.common.util.Formatters
import com.velqor.core.designsystem.component.VelqorSecondaryButton
import com.velqor.core.designsystem.theme.AppleBlue
import com.velqor.core.designsystem.theme.LocalVelqorColors
import com.velqor.core.filesystem.model.FileItem
import com.velqor.core.security.hash.Sha256Hasher

@Composable
fun FileDetailsDialog(
    item: FileItem,
    onDismiss: () -> Unit
) {
    val colors = LocalVelqorColors.current
    val clipboardManager = LocalClipboardManager.current
    var hashResult by remember { mutableStateOf<String?>(null) }
    var isCalculatingHash by remember { mutableStateOf(false) }

    LaunchedEffect(item.path) {
        if (!item.isDirectory) {
            isCalculatingHash = true
            val hasher = Sha256Hasher(DefaultDispatcherProvider())
            val res = hasher.calculateHash(item.path)
            hashResult = res.getOrNull()
            isCalculatingHash = false
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceElevated,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = AppleBlue, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Thông tin tệp tin",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            DetailRow(label = "Tên", value = item.name)
            DetailRow(label = "Đường dẫn", value = item.path)
            DetailRow(label = "Kích thước", value = Formatters.formatFileSize(item.sizeBytes))
            DetailRow(label = "Loại tệp", value = item.type.name)
            DetailRow(label = "Ngày sửa", value = Formatters.formatDate(item.lastModifiedMs))

            if (!item.isDirectory) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Fingerprint, null, tint = colors.accent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mã băm SHA-256:", fontSize = 12.sp, color = colors.textSecondary)
                }
                Spacer(modifier = Modifier.height(4.dp))

                if (isCalculatingHash) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đang tính toán mã băm...", fontSize = 12.sp, color = colors.textTertiary)
                    }
                } else if (hashResult != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = hashResult!!,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(hashResult!!))
                        }) {
                            Icon(Icons.Default.ContentCopy, "Chép SHA-256", tint = colors.accent)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            VelqorSecondaryButton(
                text = "Đóng",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val colors = LocalVelqorColors.current
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, fontSize = 11.sp, color = colors.textSecondary)
        Text(text = value, fontSize = 13.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
    }
}
