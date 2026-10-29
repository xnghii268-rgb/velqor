package com.velqor.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.velqor.core.common.util.Formatters
import com.velqor.core.designsystem.theme.IosBody
import com.velqor.core.designsystem.theme.IosFileIconShape
import com.velqor.core.designsystem.theme.IosFootnote
import com.velqor.core.designsystem.theme.LocalVelqorColors
import com.velqor.core.filesystem.model.FileItem
import com.velqor.core.filesystem.model.FileType
import java.io.File

enum class IosGroupPosition {
    ONLY,
    FIRST,
    MIDDLE,
    LAST
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IosFileRow(
    item: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    position: IosGroupPosition,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVelqorColors.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "file_row_scale"
    )

    val shape: Shape = when (position) {
        IosGroupPosition.ONLY -> RoundedCornerShape(16.dp)
        IosGroupPosition.FIRST -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        IosGroupPosition.MIDDLE -> RectangleShape
        IosGroupPosition.LAST -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    val rowBackground = when {
        isSelected -> colors.accent.copy(alpha = 0.08f)
        isPressed -> colors.separator.copy(alpha = 0.15f)
        else -> colors.surfaceElevated
    }

    // Number of children if folder
    val childCount = remember(item.path, item.isDirectory) {
        if (item.isDirectory) {
            try {
                File(item.path).list()?.size ?: 0
            } catch (_: Exception) {
                0
            }
        } else {
            0
        }
    }

    val (iconVector, iconTint) = resolveSemanticIcon(item.type, colors)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(rowBackground)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // iOS Selection Checkbox (Circular)
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Row {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) colors.accent else Color.Transparent)
                            .border(
                                width = if (isSelected) 0.dp else 1.5.dp,
                                color = if (isSelected) Color.Transparent else colors.separator,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }

            // 40x40dp 10dp rounded Icon Box with semantic tint
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(IosFileIconShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Info (File Name + Metadata)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                val displayName = Formatters.middleEllipsis(item.name, 34)
                Text(
                    text = displayName,
                    style = IosBody,
                    fontWeight = FontWeight.Normal,
                    color = colors.label,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(2.dp))

                val subtitle = if (item.isDirectory) {
                    "$childCount mục · ${Formatters.formatItemDate(item.lastModifiedMs)}"
                } else {
                    "${Formatters.formatFileSize(item.sizeBytes)} · ${Formatters.formatItemDate(item.lastModifiedMs)}"
                }

                Text(
                    text = subtitle,
                    style = IosFootnote,
                    color = colors.secondaryLabel,
                    maxLines = 1
                )
            }

            // Right Chevron for folders (when not in selection mode)
            if (item.isDirectory && !isSelectionMode) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colors.secondaryLabel.copy(alpha = 0.4f)
                )
            }
        }

        // Inset Separator (0.5dp, inset from text start, omitted on LAST / ONLY item)
        if (position != IosGroupPosition.LAST && position != IosGroupPosition.ONLY) {
            val separatorInset = if (isSelectionMode) 102.dp else 68.dp
            HorizontalDivider(
                thickness = 0.5.dp,
                color = colors.separator,
                modifier = Modifier.padding(start = separatorInset)
            )
        }
    }
}

private fun resolveSemanticIcon(
    type: FileType,
    colors: com.velqor.core.designsystem.theme.VelqorColors
): Pair<ImageVector, Color> {
    return when (type) {
        FileType.FOLDER -> Icons.Outlined.Folder to colors.iconFolder
        FileType.IMAGE -> Icons.Outlined.Image to colors.iconImage
        FileType.VIDEO -> Icons.Outlined.Movie to colors.iconVideo
        FileType.AUDIO -> Icons.Outlined.MusicNote to colors.iconAudio
        FileType.DOCUMENT -> Icons.Outlined.Description to colors.iconDocument
        FileType.ARCHIVE_ZIP, FileType.ARCHIVE_7Z, FileType.ARCHIVE_RAR, FileType.ARCHIVE_TAR ->
            Icons.Outlined.Archive to colors.iconArchive
        FileType.APK -> Icons.Outlined.Android to colors.iconApk
        FileType.CODE -> Icons.Outlined.Code to colors.iconCode
        FileType.UNKNOWN -> Icons.AutoMirrored.Outlined.InsertDriveFile to colors.secondaryLabel
    }
}
