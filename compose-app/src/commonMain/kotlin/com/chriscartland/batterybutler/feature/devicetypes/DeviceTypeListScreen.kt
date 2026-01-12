package com.chriscartland.batterybutler.feature.devicetypes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chriscartland.batterybutler.presentation.feature.devicetypes.DeviceTypeListContent
import com.chriscartland.batterybutler.viewmodel.devicetypes.DeviceTypeListViewModel

@Composable
fun DeviceTypeListScreen(
    viewModel: DeviceTypeListViewModel,
    onEditType: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DeviceTypeListContent(
        state = uiState,
        onEditType = onEditType,
        onSortOptionSelected = { option ->
            viewModel.onSortOptionSelected(option)
        },
        onGroupOptionSelected = { option ->
            viewModel.onGroupOptionSelected(option)
        },
        onSortDirectionToggle = { viewModel.toggleSortDirection() },
        onGroupDirectionToggle = { viewModel.toggleGroupDirection() },
        modifier = modifier,
    )
}
