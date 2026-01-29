package com.chriscartland.batterybutler.viewmodel.devicetypes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeGroupOption
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeListUiState
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeSortOption
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.viewmodel.defaultWhileSubscribed
import com.chriscartland.batterybutler.viewmodel.toSortedMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject
import kotlin.comparisons.naturalOrder
import kotlin.comparisons.reverseOrder

@Inject
class DeviceTypeListViewModel(
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
) : ViewModel() {
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
            var sortedList = when (config.sort) {
                DeviceTypeSortOption.NAME -> list.sortedBy { it.name }
                DeviceTypeSortOption.BATTERY_TYPE -> list.sortedWith(compareBy<DeviceType> { it.batteryType ?: "" }.thenBy { it.name })
            }
            if (!config.isSortAscending) {
                sortedList = sortedList.reversed()
            }

            val groupedList = when (config.group) {
                DeviceTypeGroupOption.NONE -> mapOf("All Types" to sortedList)
                DeviceTypeGroupOption.BATTERY_TYPE -> sortedList.groupBy { it.batteryType ?: "Unknown Battery" }
            }

            val finalGroupedList = if (config.group != DeviceTypeGroupOption.NONE) {
                val comparator = if (config.isGroupAscending) naturalOrder<String>() else reverseOrder()
                groupedList.toSortedMap(comparator)
            } else {
                groupedList
            }

            DeviceTypeListUiState.Success(
                groupedTypes = finalGroupedList,
                sortOption = config.sort,
                groupOption = config.group,
                isSortAscending = config.isSortAscending,
                isGroupAscending = config.isGroupAscending,
            )
        }.stateIn(
            scope = viewModelScope,
            started = defaultWhileSubscribed(),
            initialValue = DeviceTypeListUiState.Success(emptyMap()),
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
}

private data class DeviceTypeSortConfig(
    val sort: DeviceTypeSortOption,
    val group: DeviceTypeGroupOption,
    val isSortAscending: Boolean,
    val isGroupAscending: Boolean,
)
