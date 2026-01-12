package com.chriscartland.batterybutler.feature.devicedetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chriscartland.batterybutler.presenter.feature.devicedetail.DeviceDetailContent
import com.chriscartland.batterybutler.presenter.models.devicedetail.DeviceDetailUiState
import com.chriscartland.batterybutler.viewmodel.devicedetail.DeviceDetailViewModel

@Composable
fun DeviceDetailScreen(
    viewModel: DeviceDetailViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DeviceDetailContent(
        state = uiState,
        onRecordReplacement = { viewModel.recordReplacement() },
        onBack = onBack,
        onEdit = onEdit,
        onEventClick = onEventClick,
        modifier = modifier,
    )
}
