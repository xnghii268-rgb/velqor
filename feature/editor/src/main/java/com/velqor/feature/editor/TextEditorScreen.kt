package com.velqor.feature.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velqor.core.designsystem.component.VelqorTopBar
import com.velqor.core.designsystem.theme.LocalVelqorColors
import java.io.File
import java.io.FileOutputStream

@Composable
fun TextEditorScreen(
    filePath: String,
    onNavigateBack: () -> Unit
) {
    val colors = LocalVelqorColors.current
    var textContent by remember { mutableStateOf("") }
    var initialContent by remember { mutableStateOf("") }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    val file = File(filePath)

    LaunchedEffect(filePath) {
        if (file.exists() && file.isFile) {
            val content = file.readText(Charsets.UTF_8)
            textContent = content
            initialContent = content
        }
    }

    fun saveFile() {
        try {
            // Atomic save: write to temp file then rename
            val tempFile = File(file.parentFile, "${file.name}.tmp")
            FileOutputStream(tempFile).use { out ->
                out.write(textContent.toByteArray(Charsets.UTF_8))
            }
            if (tempFile.renameTo(file)) {
                initialContent = textContent
                hasUnsavedChanges = false
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    BackHandler {
        if (textContent != initialContent) {
            showExitDialog = true
        } else {
            onNavigateBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        VelqorTopBar(
            title = file.name,
            navigationIcon = {
                IconButton(onClick = {
                    if (textContent != initialContent) showExitDialog = true else onNavigateBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = colors.textPrimary
                    )
                }
            },
            actions = {
                IconButton(onClick = { saveFile() }) {
                    Icon(Icons.Default.Save, "Lưu", tint = colors.accent)
                }
            }
        )

        // Line Count bar
        val lineCount = textContent.lines().size
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.surfaceElevated
        ) {
            Text(
                text = "Tổng: $lineCount dòng • UTF-8",
                fontSize = 12.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // Code Editor Area with Line Numbers
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Line numbers column
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .width(42.dp)
                    .fillMaxHeight()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.End
            ) {
                for (i in 1..lineCount) {
                    Text(
                        text = "$i",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = colors.textTertiary,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Main Editor text
            BasicTextField(
                value = textContent,
                onValueChange = {
                    textContent = it
                    hasUnsavedChanges = (it != initialContent)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = colors.textPrimary
                ),
                cursorBrush = SolidColor(colors.accent)
            )
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Lưu thay đổi?") },
            text = { Text("Tệp '${file.name}' có những thay đổi chưa được lưu.") },
            confirmButton = {
                TextButton(onClick = {
                    saveFile()
                    showExitDialog = false
                    onNavigateBack()
                }) {
                    Text("Lưu & Thoát")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onNavigateBack()
                }) {
                    Text("Bỏ qua")
                }
            }
        )
    }
}
