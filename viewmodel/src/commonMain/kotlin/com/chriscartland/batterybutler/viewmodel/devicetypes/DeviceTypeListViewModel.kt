package com.chriscartland.batterybutler.viewmodel.devicetypes

import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeGroupOption
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeListScreenState
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeSortOption
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.PreloadCommonTypesUseCase
import com.chriscartland.batterybutler.viewmodel.defaultWhileSubscribed
import com.chriscartland.batterybutler.viewmodel.retryableStateIn
import com.chriscartland.batterybutler.viewmodel.util.sortAndGroup
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
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

    private val retryTrigger = MutableStateFlow(0)

    fun retry() {
        retryTrigger.update { it + 1 }
    }

    val uiState: StateFlow<DeviceTypeListScreenState> = retryableStateIn(
        viewModelScope = viewModelScope,
        retryTrigger = retryTrigger,
        started = defaultWhileSubscribed(),
        initialValue = DeviceTypeListScreenState.Success(emptyMap()),
        onError = { DeviceTypeListScreenState.Error(it.message ?: "Failed to load device types") },
        source = {
            combine(
                combine(
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

                DeviceTypeListScreenState.Success(
                    groupedTypes = finalGroupedList,
                    sortOption = config.sort,
                    groupOption = config.group,
                    isSortAscending = config.isSortAscending,
                    isGroupAscending = config.isGroupAscending,
                )
            }
        },
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
        viewModelScope.coroutineScope.launch {
            when (val result = preloadCommonTypesUseCase()) {
                is Result.Success -> { /* success */ }

                is Result.Error -> {
                    _actionError.value = result.error.message
                }
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
