package com.chriscartland.batterybutler.feature.devicedetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chriscartland.batterybutler.ui.feature.devicedetail.DeviceDetailContent
import com.chriscartland.batterybutler.ui.feature.devicedetail.DeviceDetailUiState

@Composable
fun DeviceDetailScreen(
    viewModel: DeviceDetailViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val coreUiState = when (val state = uiState) {
        com.chriscartland.batterybutler.feature.devicedetail.DeviceDetailUiState.Loading -> DeviceDetailUiState.Loading
        com.chriscartland.batterybutler.feature.devicedetail.DeviceDetailUiState.NotFound -> DeviceDetailUiState.NotFound
        is com.chriscartland.batterybutler.feature.devicedetail.DeviceDetailUiState.Success -> DeviceDetailUiState.Success(
            device = state.device,
            deviceType = state.deviceType,
            events = state.events,
        )
    }

    DeviceDetailContent(
        state = coreUiState,
        onRecordReplacement = { viewModel.recordReplacement() },
        onBack = onBack,
        onEdit = onEdit,
        onEventClick = onEventClick,
        modifier = modifier,
    )
}
