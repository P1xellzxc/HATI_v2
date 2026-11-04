package com.hativ2.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small       = RoundedCornerShape(8.dp),
    medium      = RoundedCornerShape(12.dp),
    large       = RoundedCornerShape(16.dp),
    extraLarge  = RoundedCornerShape(24.dp)
)

// Named aliases for semantic use in components
val ShapeCard    = RoundedCornerShape(12.dp)
val ShapeButton  = RoundedCornerShape(8.dp)
val ShapeChip    = RoundedCornerShape(100.dp)
val ShapeInput   = RoundedCornerShape(10.dp)
val ShapeAvatar  = RoundedCornerShape(100.dp)
val ShapeHero    = RoundedCornerShape(20.dp)
