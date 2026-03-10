package com.chriscartland.batterybutler.viewmodel.devicetypes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeGroupOption
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeListUiState
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeSortOption
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.PreloadCommonTypesUseCase
import com.chriscartland.batterybutler.viewmodel.defaultWhileSubscribed
import com.chriscartland.batterybutler.viewmodel.safeStateIn
import com.chriscartland.batterybutler.viewmodel.util.sortAndGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@Inject
class DeviceTypeListViewModel(
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
    private val preloadCommonTypesUseCase: PreloadCommonTypesUseCase,
) : ViewModel() {
    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError

    fun dismissActionError() {
        _actionError.value = null
    }

    private val sortOptionFlow = MutableStateFlow(DeviceTypeSortOption.NAME)
    private val groupOptionFlow = MutableStateFlow(DeviceTypeGroupOption.NONE)
    private val isSortAscendingFlow = MutableStateFlow(true)
    private val isGroupAscendingFlow = MutableStateFlow(true)

    val uiState: StateFlow<DeviceTypeListUiState> = kotlinx.coroutines.flow
        .combine(
            kotlinx.coroutines.flow.combine(
                sortOptionFlow,
                groupOptionFlow,
                isSortAscendingFlow,
                isGroupAscendingFlow,
            ) { sort, group, isSortAscending, isGroupAscending ->
                DeviceTypeSortConfig(sort, group, isSortAscending, isGroupAscending)
            },
            getDeviceTypesUseCase(),
        ) { config, list ->
            val sortComparator = when (config.sort) {
                DeviceTypeSortOption.NAME -> compareBy<DeviceType> { it.name }
                DeviceTypeSortOption.BATTERY_TYPE -> compareBy<DeviceType> { it.batteryType }.thenBy { it.name }
            }

            val groupKeySelector = when (config.group) {
                DeviceTypeGroupOption.NONE -> null
                DeviceTypeGroupOption.BATTERY_TYPE -> { type: DeviceType -> type.batteryType }
            }

            val finalGroupedList = sortAndGroup(
                items = list,
                sortComparator = sortComparator,
                isSortAscending = config.isSortAscending,
                groupKeySelector = groupKeySelector,
                defaultGroupName = "All Types",
                isGroupAscending = config.isGroupAscending,
            )

            DeviceTypeListUiState.Success(
                groupedTypes = finalGroupedList,
                sortOption = config.sort,
                groupOption = config.group,
                isSortAscending = config.isSortAscending,
                isGroupAscending = config.isGroupAscending,
            )
        }.safeStateIn(
            scope = viewModelScope,
            started = defaultWhileSubscribed(),
            initialValue = DeviceTypeListUiState.Success(emptyMap()),
            onError = { DeviceTypeListUiState.Error(it.message ?: "Failed to load device types") },
        )

    fun onSortOptionSelected(option: DeviceTypeSortOption) {
        sortOptionFlow.value = option
    }

    fun onGroupOptionSelected(option: DeviceTypeGroupOption) {
        groupOptionFlow.value = option
    }

    fun toggleSortDirection() {
        isSortAscendingFlow.value = !isSortAscendingFlow.value
    }

    fun toggleGroupDirection() {
        isGroupAscendingFlow.value = !isGroupAscendingFlow.value
    }

    fun preloadCommonTypes() {
        viewModelScope.launch {
            when (val result = preloadCommonTypesUseCase()) {
                is Result.Success -> { /* success */ }
                is Result.Error -> _actionError.value = result.error.message
            }
        }
    }
}

private data class DeviceTypeSortConfig(
    val sort: DeviceTypeSortOption,
    val group: DeviceTypeGroupOption,
    val isSortAscending: Boolean,
    val isGroupAscending: Boolean,
)
