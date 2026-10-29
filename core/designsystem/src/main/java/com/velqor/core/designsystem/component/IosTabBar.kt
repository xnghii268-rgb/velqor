package com.velqor.core.designsystem.component

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.velqor.core.designsystem.theme.IosCaption
import com.velqor.core.designsystem.theme.LocalVelqorColors

@Composable
fun IosTabBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val colors = LocalVelqorColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceElevated.copy(alpha = 0.92f))
    ) {
        // 0.5dp top hair-line
        HorizontalDivider(
            thickness = 0.5.dp,
            color = colors.separator
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(49.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}

@Composable
fun RowScope.IosTabBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalVelqorColors.current
    val itemColor = if (selected) colors.accent else colors.secondaryLabel

    Column(
        modifier = modifier
            .weight(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = itemColor
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            style = IosCaption,
            color = itemColor
        )
    }
}
