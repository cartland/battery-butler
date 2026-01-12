package com.chriscartland.batterybutler.feature.devicetypes

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.chriscartland.batterybutler.presenter.feature.devicetypes.EditDeviceTypeContent
import com.chriscartland.batterybutler.presenter.models.devicetypes.EditDeviceTypeUiState
import com.chriscartland.batterybutler.viewmodel.devicetypes.EditDeviceTypeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDeviceTypeScreen(
    viewModel: EditDeviceTypeViewModel,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    EditDeviceTypeContent(
        uiState = uiState,
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
