package com.velqor.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val IosGroupedListShape = RoundedCornerShape(16.dp)
val IosSheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
val IosPillShape = RoundedCornerShape(999.dp)
val IosFileIconShape = RoundedCornerShape(10.dp)

val VelqorShapes = Shapes(
    small = IosFileIconShape,
    medium = IosGroupedListShape,
    large = IosSheetShape,
    extraLarge = IosPillShape
)
