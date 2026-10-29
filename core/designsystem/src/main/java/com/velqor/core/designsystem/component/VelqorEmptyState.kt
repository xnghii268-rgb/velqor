package com.velqor.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.velqor.core.designsystem.theme.IosBody
import com.velqor.core.designsystem.theme.IosHeadline
import com.velqor.core.designsystem.theme.IosSubhead
import com.velqor.core.designsystem.theme.LocalVelqorColors

@Composable
fun VelqorEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.FolderOpen,
    actionButton: @Composable (() -> Unit)? = null
) {
    val colors = LocalVelqorColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = colors.secondaryLabel.copy(alpha = 0.4f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = IosHeadline,
            color = colors.label,
            textAlign = TextAlign.Center
        )

        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = IosSubhead,
                color = colors.secondaryLabel,
                textAlign = TextAlign.Center
            )
        }

        if (actionButton != null) {
            Spacer(modifier = Modifier.height(20.dp))
            actionButton()
        }
    }
}

@Composable
fun VelqorLoadingIndicator(
    modifier: Modifier = Modifier
) {
    val colors = LocalVelqorColors.current

    CircularProgressIndicator(
        modifier = modifier.size(36.dp),
        color = colors.accent,
        strokeWidth = 3.dp
    )
}
