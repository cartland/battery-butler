package com.chriscartland.batterybutler.presentationcore.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ButlerColors(
    val batteryWarning: Color,
)

val LocalButlerColors = staticCompositionLocalOf<ButlerColors> {
    error("No ButlerColors provided. Wrap content in BatteryButlerTheme.")
}
