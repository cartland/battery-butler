package com.chriscartland.batterybutler.feature.devicetypes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chriscartland.batterybutler.ui.feature.devicetypes.DeviceTypeListContent
import com.chriscartland.batterybutler.ui.feature.devicetypes.DeviceTypeListUiState
import com.chriscartland.batterybutler.ui.feature.devicetypes.UiDeviceTypeGroupOption
import com.chriscartland.batterybutler.ui.feature.devicetypes.UiDeviceTypeSortOption
import com.chriscartland.batterybutler.feature.devicetypes.DeviceTypeGroupOption as VmGroupOption
import com.chriscartland.batterybutler.feature.devicetypes.DeviceTypeListUiState as VmUiState
import com.chriscartland.batterybutler.feature.devicetypes.DeviceTypeSortOption as VmSortOption

@Composable
fun DeviceTypeListScreen(
    viewModel: DeviceTypeListViewModel,
    onEditType: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val coreUiState = when (val state = uiState) {
        VmUiState.Loading -> DeviceTypeListUiState.Loading
        is VmUiState.Success -> {
            DeviceTypeListUiState.Success(
                groupedTypes = state.groupedTypes,
                sortOption = when (state.sortOption) {
                    VmSortOption.NAME -> UiDeviceTypeSortOption.NAME
                    VmSortOption.BATTERY_TYPE -> UiDeviceTypeSortOption.BATTERY_TYPE
                },
                groupOption = when (state.groupOption) {
                    VmGroupOption.NONE -> UiDeviceTypeGroupOption.NONE
                    VmGroupOption.BATTERY_TYPE -> UiDeviceTypeGroupOption.BATTERY_TYPE
                },
                isSortAscending = state.isSortAscending,
                isGroupAscending = state.isGroupAscending,
            )
        }
    }

    DeviceTypeListContent(
        state = coreUiState,
        onEditType = onEditType,
        onSortOptionSelected = { option ->
            val vmOption = when (option) {
                UiDeviceTypeSortOption.NAME -> VmSortOption.NAME
                UiDeviceTypeSortOption.BATTERY_TYPE -> VmSortOption.BATTERY_TYPE
            }
            viewModel.onSortOptionSelected(vmOption)
        },
        onGroupOptionSelected = { option ->
            val vmOption = when (option) {
                UiDeviceTypeGroupOption.NONE -> VmGroupOption.NONE
                UiDeviceTypeGroupOption.BATTERY_TYPE -> VmGroupOption.BATTERY_TYPE
            }
            viewModel.onGroupOptionSelected(vmOption)
        },
        onSortDirectionToggle = { viewModel.toggleSortDirection() },
        onGroupDirectionToggle = { viewModel.toggleGroupDirection() },
        modifier = modifier,
    )
}
