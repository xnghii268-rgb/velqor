package com.velqor.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.velqor.core.designsystem.theme.IosGroupedListShape
import com.velqor.core.designsystem.theme.LocalVelqorColors

@Composable
fun IosGroupedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalVelqorColors.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(IosGroupedListShape),
        shape = IosGroupedListShape,
        color = colors.surfaceElevated,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}
