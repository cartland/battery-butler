package com.chriscartland.batterybutler.composeapp.feature.devicetypes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chriscartland.batterybutler.presentationfeature.devicetypes.DeviceTypeDetailContent
import com.chriscartland.batterybutler.viewmodel.devicetypes.DeviceTypeDetailViewModel

@Composable
fun DeviceTypeDetailScreen(
    viewModel: DeviceTypeDetailViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeviceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DeviceTypeDetailContent(
        state = uiState,
        onBack = onBack,
        onEdit = onEdit,
        onDeviceClick = onDeviceClick,
        modifier = modifier,
    )
}
