package com.chriscartland.batterybutler.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

val LocalAiAvailable = staticCompositionLocalOf { false }
val LocalAiAction = staticCompositionLocalOf<() -> Unit> { {} }
