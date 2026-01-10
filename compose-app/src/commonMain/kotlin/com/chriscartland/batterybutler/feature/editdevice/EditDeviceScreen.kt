package com.chriscartland.batterybutler.feature.editdevice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.chriscartland.batterybutler.ui.feature.editdevice.EditDeviceContent
import com.chriscartland.batterybutler.ui.feature.editdevice.EditDeviceUiState

@Composable
fun EditDeviceScreen(
    viewModel: EditDeviceViewModel,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    val coreUiState = when (val state = uiState) {
        com.chriscartland.batterybutler.feature.editdevice.EditDeviceUiState.Loading -> EditDeviceUiState.Loading
        com.chriscartland.batterybutler.feature.editdevice.EditDeviceUiState.NotFound -> EditDeviceUiState.NotFound
        is com.chriscartland.batterybutler.feature.editdevice.EditDeviceUiState.Success -> EditDeviceUiState.Success(
            device = state.device,
            deviceTypes = state.deviceTypes,
        )
    }

    EditDeviceContent(
        uiState = coreUiState,
        onSave = { input ->
            viewModel.updateDevice(input)
            onBack()
        },
        onDelete = {
            viewModel.deleteDevice()
            onDelete()
        },
        onBack = onBack,
        modifier = modifier,
    )
}
