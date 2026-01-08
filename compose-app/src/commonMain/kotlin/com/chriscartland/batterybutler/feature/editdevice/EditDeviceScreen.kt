package com.chriscartland.batterybutler.feature.editdevice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.chriscartland.batterybutler.ui.feature.editdevice.EditDeviceContent
import com.chriscartland.batterybutler.ui.feature.editdevice.EditDeviceUiState
import com.chriscartland.batterybutler.viewmodel.editdevice.EditDeviceUiState as VmUiState
import com.chriscartland.batterybutler.viewmodel.editdevice.EditDeviceViewModel

@Composable
fun EditDeviceScreen(
    viewModel: EditDeviceViewModel,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    val coreUiState = when (val state = uiState) {
        VmUiState.Loading -> EditDeviceUiState.Loading
        VmUiState.NotFound -> EditDeviceUiState.NotFound
        is VmUiState.Success -> EditDeviceUiState.Success(
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
