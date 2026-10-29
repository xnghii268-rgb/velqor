package com.velqor.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.velqor.core.designsystem.theme.LocalVelqorColors

@Composable
fun VelqorCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = LocalVelqorColors.current

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceElevated,
            border = BorderStroke(0.5.dp, colors.separator.copy(alpha = 0.5f))
        ) {
            Box(modifier = Modifier.padding(14.dp)) {
                content()
            }
        }
    } else {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceElevated,
            border = BorderStroke(0.5.dp, colors.separator.copy(alpha = 0.5f))
        ) {
            Box(modifier = Modifier.padding(14.dp)) {
                content()
            }
        }
    }
}
