package com.chriscartland.blanket.ui.components

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    // No-op on iOS as there is no system back button
}
