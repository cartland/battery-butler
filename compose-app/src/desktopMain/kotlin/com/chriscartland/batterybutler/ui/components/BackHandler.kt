package com.chriscartland.batterybutler.presenter.core.components

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    // No-op on desktop
}
