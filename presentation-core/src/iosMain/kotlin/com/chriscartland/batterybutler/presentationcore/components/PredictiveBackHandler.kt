package com.chriscartland.batterybutler.presentationcore.components

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

@Composable
actual fun PredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (Flow<BackGestureEvent>) -> Unit,
) {
    // No-op on iOS — no system back gesture.
}
