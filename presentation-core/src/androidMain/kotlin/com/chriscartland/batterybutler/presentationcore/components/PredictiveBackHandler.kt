package com.chriscartland.batterybutler.presentationcore.components

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Composable
actual fun PredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (Flow<BackGestureEvent>) -> Unit,
) {
    androidx.activity.compose.PredictiveBackHandler(enabled = enabled) { flow ->
        onBack(flow.map { BackGestureEvent(progress = it.progress) })
    }
}
