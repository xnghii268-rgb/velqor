package com.velqor.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velqor.core.designsystem.component.VelqorTopBar
import com.velqor.core.designsystem.theme.AppleBlue
import com.velqor.core.designsystem.theme.LocalVelqorColors

@Composable
fun SettingsScreen() {
    val colors = LocalVelqorColors.current
    var showHiddenFiles by remember { mutableStateOf(false) }
    var useBiometric by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        VelqorTopBar(title = "Cài đặt")

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "HIỂN THỊ & QUẢN LÝ",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textTertiary,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceElevated
            ) {
                Column {
                    SettingsSwitchItem(
                        icon = Icons.Default.Visibility,
                        title = "Hiển thị tệp tin ẩn",
                        subtitle = "Bao gồm các tệp và thư mục bắt đầu bằng dấu chấm",
                        checked = showHiddenFiles,
                        onCheckedChange = { showHiddenFiles = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "BẢO MẬT & KHO DỮ LIỆU",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textTertiary,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceElevated
            ) {
                Column {
                    SettingsSwitchItem(
                        icon = Icons.Default.Security,
                        title = "Khóa sinh trắc học",
                        subtitle = "Yêu cầu vân tay hoặc mã PIN khi mở Kho bảo mật",
                        checked = useBiometric,
                        onCheckedChange = { useBiometric = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "THÔNG TIN ỨNG DỤNG",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textTertiary,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceElevated
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = AppleBlue, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("VELQOR", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Phiên bản 2.0.0 (Build 2) • Chuẩn Apple Minimalist Pro", fontSize = 12.sp, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalVelqorColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = colors.accent, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = colors.textSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.surface,
                checkedTrackColor = colors.accent
            )
        )
    }
}
