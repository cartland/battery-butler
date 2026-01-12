package com.chriscartland.batterybutler.feature.editdevice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.chriscartland.batterybutler.presentation.feature.editdevice.EditDeviceContent
import com.chriscartland.batterybutler.presentation.models.editdevice.EditDeviceUiState
import com.chriscartland.batterybutler.viewmodel.editdevice.EditDeviceViewModel

@Composable
fun EditDeviceScreen(
    viewModel: EditDeviceViewModel,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onManageDeviceTypesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    EditDeviceContent(
        uiState = uiState,
        onSave = { input ->
            viewModel.updateDevice(input)
            onBack()
        },
        onDelete = {
            viewModel.deleteDevice()
            onDelete()
        },
        onManageDeviceTypesClick = onManageDeviceTypesClick,
        onBack = onBack,
        modifier = modifier,
    )
}
