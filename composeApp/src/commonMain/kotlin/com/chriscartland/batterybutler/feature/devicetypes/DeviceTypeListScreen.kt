package com.chriscartland.batterybutler.feature.devicetypes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chriscartland.batterybutler.ui.feature.devicetypes.DeviceTypeListScreen
import com.chriscartland.batterybutler.ui.feature.devicetypes.DeviceTypeListUiState

@Composable
fun DeviceTypeListScreen(
    viewModel: DeviceTypeListViewModel,
    onEditType: (String) -> Unit,
    onAddType: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Map ViewModel UiState to UI Core UiState
    val coreUiState = when (val state = uiState) {
        com.chriscartland.batterybutler.feature.devicetypes.DeviceTypeListUiState.Loading -> DeviceTypeListUiState.Loading
        is com.chriscartland.batterybutler.feature.devicetypes.DeviceTypeListUiState.Success -> DeviceTypeListUiState.Success(
            state.deviceTypes,
        )
    }

    DeviceTypeListScreen(
        state = coreUiState,
        onEditType = onEditType,
        onAddType = onAddType,
        onBack = onBack,
        modifier = modifier,
    )
}
