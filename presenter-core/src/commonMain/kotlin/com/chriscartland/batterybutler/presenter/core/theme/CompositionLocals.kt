package com.chriscartland.batterybutler.presenter.core.theme

import androidx.compose.runtime.staticCompositionLocalOf

val LocalAiAvailable = staticCompositionLocalOf { false }
val LocalAiAction = staticCompositionLocalOf<() -> Unit> { {} }
