package com.chriscartland.batterybutler.feature.devicetypes

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.chriscartland.batterybutler.ui.feature.devicetypes.EditDeviceTypeContent
import com.chriscartland.batterybutler.ui.feature.devicetypes.EditDeviceTypeUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDeviceTypeScreen(
    viewModel: EditDeviceTypeViewModel,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    val coreUiState = when (val state = uiState) {
        com.chriscartland.batterybutler.feature.devicetypes.EditDeviceTypeUiState.Loading -> EditDeviceTypeUiState.Loading
        com.chriscartland.batterybutler.feature.devicetypes.EditDeviceTypeUiState.NotFound -> EditDeviceTypeUiState.NotFound
        is com.chriscartland.batterybutler.feature.devicetypes.EditDeviceTypeUiState.Success -> EditDeviceTypeUiState.Success(
            deviceType = state.deviceType,
        )
    }

    EditDeviceTypeContent(
        uiState = coreUiState,
        onSave = { input ->
            viewModel.updateDeviceType(input)
            onBack()
        },
        onDelete = {
            viewModel.deleteDeviceType()
            onDelete()
        },
        onBack = onBack,
        modifier = modifier,
    )
}
