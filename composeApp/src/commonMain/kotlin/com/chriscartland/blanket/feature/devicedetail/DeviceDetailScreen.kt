package com.chriscartland.blanket.feature.devicedetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chriscartland.blanket.ui.feature.devicedetail.DeviceDetailScreen
import com.chriscartland.blanket.ui.feature.devicedetail.DeviceDetailUiState

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
        com.chriscartland.blanket.feature.devicedetail.DeviceDetailUiState.Loading -> DeviceDetailUiState.Loading
        com.chriscartland.blanket.feature.devicedetail.DeviceDetailUiState.NotFound -> DeviceDetailUiState.NotFound
        is com.chriscartland.blanket.feature.devicedetail.DeviceDetailUiState.Success -> DeviceDetailUiState.Success(
            device = state.device,
            deviceType = state.deviceType,
            events = state.events,
        )
    }

    DeviceDetailScreen(
        state = coreUiState,
        onRecordReplacement = { viewModel.recordReplacement() },
        onBack = onBack,
        onEdit = onEdit,
        onEventClick = onEventClick,
        modifier = modifier,
    )
}
