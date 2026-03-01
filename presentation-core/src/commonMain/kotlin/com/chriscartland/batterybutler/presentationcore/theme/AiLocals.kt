package com.chriscartland.batterybutler.presentationcore.theme

import androidx.compose.runtime.staticCompositionLocalOf

val LocalAiAvailable = staticCompositionLocalOf { false }
val LocalAiAction = staticCompositionLocalOf<() -> Unit> { {} }
