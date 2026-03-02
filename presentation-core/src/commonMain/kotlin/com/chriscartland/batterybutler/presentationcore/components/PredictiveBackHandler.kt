package com.chriscartland.batterybutler.presentationcore.components

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

data class BackGestureEvent(
    val progress: Float,
)

@Composable
expect fun PredictiveBackHandler(
    enabled: Boolean = true,
    onBack: suspend (Flow<BackGestureEvent>) -> Unit,
)
