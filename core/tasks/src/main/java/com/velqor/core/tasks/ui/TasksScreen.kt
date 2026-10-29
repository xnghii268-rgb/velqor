package com.velqor.core.tasks.ui

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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.velqor.core.designsystem.theme.AppleGreen
import com.velqor.core.designsystem.theme.AppleRed
import com.velqor.core.designsystem.theme.LocalVelqorColors
import com.velqor.core.tasks.model.TaskInfo
import com.velqor.core.tasks.model.TaskStatus
import com.velqor.core.tasks.queue.TaskManager

@Composable
fun TasksScreen(
    taskManager: TaskManager
) {
    val tasks by taskManager.tasks.collectAsState()
    val colors = LocalVelqorColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        VelqorTopBar(
            title = "Trung Tâm Tác Vụ",
            actions = {
                if (tasks.any { it.status == TaskStatus.COMPLETED || it.status == TaskStatus.FAILED }) {
                    IconButton(onClick = { taskManager.clearCompleted() }) {
                        Icon(Icons.Default.ClearAll, "Xóa lịch sử", tint = colors.textPrimary)
                    }
                }
            }
        )

        if (tasks.isEmpty()) {
            VelqorEmptyState(
                title = "Không có tác vụ nào",
                subtitle = "Các tiến trình nén, giải nén, mã hóa sẽ hiển thị tại đây.",
                icon = Icons.Default.PendingActions
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(tasks, key = { it.id }) { task ->
                    TaskItemCard(
                        task = task,
                        onCancel = { taskManager.cancelTask(task.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskItemCard(
    task: TaskInfo,
    onCancel: () -> Unit
) {
    val colors = LocalVelqorColors.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceElevated
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (statusIcon, statusColor) = when (task.status) {
                    TaskStatus.RUNNING -> Icons.Default.Sync to AppleBlue
                    TaskStatus.COMPLETED -> Icons.Default.CheckCircle to AppleGreen
                    TaskStatus.FAILED -> Icons.Default.Error to AppleRed
                    TaskStatus.CANCELLED -> Icons.Default.Cancel to Color.Gray
                    TaskStatus.PENDING -> Icons.Default.PendingActions to colors.textTertiary
                }

                Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = task.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (task.status == TaskStatus.RUNNING) {
                    IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Cancel, "Hủy", tint = colors.textTertiary)
                    }
                }
            }

            if (task.status == TaskStatus.RUNNING) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { task.progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = colors.accent,
                    trackColor = colors.surfaceSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                val speedOrDetail = if (task.totalBytes > 0) {
                    "${(task.progressPercent * 100).toInt()}% • ${Formatters.formatFileSize(task.bytesProcessed)} / ${Formatters.formatFileSize(task.totalBytes)}"
                } else {
                    "${(task.progressPercent * 100).toInt()}%"
                }

                Text(
                    text = speedOrDetail,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            } else if (task.errorMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.errorMessage,
                    fontSize = 12.sp,
                    color = AppleRed
                )
            }
        }
    }
}
