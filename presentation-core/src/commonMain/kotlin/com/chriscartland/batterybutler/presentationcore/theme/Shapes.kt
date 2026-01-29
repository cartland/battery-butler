package com.chriscartland.batterybutler.presentationcore.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 shape tokens for Battery Butler.
 *
 * Use these via MaterialTheme.shapes (e.g., MaterialTheme.shapes.small)
 * rather than hardcoding shape values directly.
 */
val BatteryButlerShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
