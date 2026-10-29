package com.velqor.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.velqor.core.designsystem.theme.IosSubhead
import com.velqor.core.designsystem.theme.LocalVelqorColors

data class BreadcrumbItem(
    val name: String,
    val path: String
)

@Composable
fun IosBreadcrumbBar(
    currentPath: String,
    onNavigateToPath: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVelqorColors.current
    val scrollState = rememberScrollState()

    // Parse path into breadcrumbs
    // Standard android primary storage: /storage/emulated/0
    val items = parseBreadcrumbs(currentPath)

    LaunchedEffect(items.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { index, item ->
            val isLast = index == items.size - 1

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = !isLast) {
                        onNavigateToPath(item.path)
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = item.name,
                    style = IosSubhead,
                    fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isLast) colors.label else colors.secondaryLabel
                )
            }

            if (!isLast) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = colors.secondaryLabel.copy(alpha = 0.5f)
                )
            }
        }
    }
}

private fun parseBreadcrumbs(path: String): List<BreadcrumbItem> {
    val rootPrefix = "/storage/emulated/0"
    val items = mutableListOf<BreadcrumbItem>()

    if (path == rootPrefix || path.isEmpty() || path == "/") {
        items.add(BreadcrumbItem("Bộ nhớ trong", rootPrefix))
        return items
    }

    if (path.startsWith(rootPrefix)) {
        items.add(BreadcrumbItem("Bộ nhớ trong", rootPrefix))
        val sub = path.removePrefix(rootPrefix).trim('/')
        if (sub.isNotEmpty()) {
            val parts = sub.split('/')
            var currentAccum = rootPrefix
            for (p in parts) {
                currentAccum = "$currentAccum/$p"
                items.add(BreadcrumbItem(p, currentAccum))
            }
        }
    } else {
        // Fallback for custom or SD card path
        val parts = path.split('/').filter { it.isNotEmpty() }
        var currentAccum = ""
        for (p in parts) {
            currentAccum = "$currentAccum/$p"
            items.add(BreadcrumbItem(p, currentAccum))
        }
    }

    return items
}
