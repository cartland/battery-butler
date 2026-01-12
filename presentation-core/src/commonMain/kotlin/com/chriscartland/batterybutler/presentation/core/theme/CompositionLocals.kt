package com.chriscartland.batterybutler.presentation.core.theme

import androidx.compose.runtime.staticCompositionLocalOf

val LocalAiAvailable = staticCompositionLocalOf { false }
val LocalAiAction = staticCompositionLocalOf<() -> Unit> { {} }
