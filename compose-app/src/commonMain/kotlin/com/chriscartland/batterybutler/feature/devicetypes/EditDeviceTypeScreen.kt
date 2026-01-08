package com.chriscartland.batterybutler.feature.devicetypes

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.chriscartland.batterybutler.ui.feature.devicetypes.EditDeviceTypeContent
import com.chriscartland.batterybutler.ui.feature.devicetypes.EditDeviceTypeUiState
import com.chriscartland.batterybutler.viewmodel.devicetypes.EditDeviceTypeViewModel
import com.chriscartland.batterybutler.viewmodel.devicetypes.EditDeviceTypeUiState as VmUiState

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
        VmUiState.Loading -> EditDeviceTypeUiState.Loading
        VmUiState.NotFound -> EditDeviceTypeUiState.NotFound
        is VmUiState.Success -> EditDeviceTypeUiState.Success(
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
