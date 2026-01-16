package com.chriscartland.batterybutler.composeapp.components

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    // No-op on desktop
}
