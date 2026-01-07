package com.chriscartland.blanket.ui.components

import androidx.compose.runtime.Composable

@Composable
expect fun BackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
)
