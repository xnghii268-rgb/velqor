package com.velqor.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val IosGroupedListShape = RoundedCornerShape(16.dp)
val IosSheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
val IosPillShape = RoundedCornerShape(999.dp)
val IosFileIconShape = RoundedCornerShape(10.dp)
val IosActionGroupShape = RoundedCornerShape(16.dp)

// Floating Liquid Glass Bar Tokens
val FloatingBarHeight = 64.dp
val FloatingBarMargin = 20.dp
val FloatingBarMaxContentWidth = 420.dp
val GlassBlurRadius = 24.dp
const val MotionPageDurationMs = 320


val VelqorShapes = Shapes(
    small = IosFileIconShape,
    medium = IosGroupedListShape,
    large = IosSheetShape,
    extraLarge = IosPillShape
)
