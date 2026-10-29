package com.velqor.feature.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velqor.core.designsystem.component.VelqorPrimaryButton
import com.velqor.core.designsystem.component.VelqorSecondaryButton
import com.velqor.core.designsystem.theme.LocalVelqorColors

@Composable
fun CreateArchiveDialog(
    selectedFilesCount: Int,
    defaultName: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, format: String, level: Int, password: String?) -> Unit
) {
    val colors = LocalVelqorColors.current

    var archiveName by remember { mutableStateOf(defaultName) }
    var selectedFormat by remember { mutableStateOf("zip") }
    var compressionLevel by remember { mutableFloatStateOf(5f) }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceElevated,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Tạo tệp nén mới ($selectedFilesCount mục)",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Archive Name
            OutlinedTextField(
                value = archiveName,
                onValueChange = { archiveName = it },
                label = { Text("Tên tệp nén") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Format selector chips
            Text("Định dạng:", fontSize = 13.sp, color = colors.textSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Row {
                listOf("zip", "tar", "tar.gz").forEach { fmt ->
                    FilterChip(
                        selected = selectedFormat == fmt,
                        onClick = { selectedFormat = fmt },
                        label = { Text(".$fmt") },
                        modifier = Modifier.padding(end = 8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accent,
                            selectedLabelColor = androidx.compose.ui.graphics.Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Compression Level slider
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Mức độ nén: ${compressionLevel.toInt()}",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = when (compressionLevel.toInt()) {
                        0 -> "Không nén (Store)"
                        in 1..3 -> "Nhanh nhất"
                        in 4..6 -> "Chuẩn (Deflate)"
                        else -> "Tối đa (Ultra)"
                    },
                    fontSize = 12.sp,
                    color = colors.accent
                )
            }
            Slider(
                value = compressionLevel,
                onValueChange = { compressionLevel = it },
                valueRange = 0f..9f,
                steps = 8
            )

            if (selectedFormat == "zip") {
                Spacer(modifier = Modifier.height(10.dp))
                // 4. Password field (AES-256 for ZIP)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mật khẩu mã hóa AES-256 (tùy chọn)") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Actions
            Row(modifier = Modifier.fillMaxWidth()) {
                VelqorSecondaryButton(
                    text = "Hủy",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                VelqorPrimaryButton(
                    text = "Nén ngay",
                    onClick = {
                        val fullName = if (archiveName.contains('.')) archiveName else "$archiveName.$selectedFormat"
                        onConfirm(
                            fullName,
                            selectedFormat,
                            compressionLevel.toInt(),
                            password.ifEmpty { null }
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
