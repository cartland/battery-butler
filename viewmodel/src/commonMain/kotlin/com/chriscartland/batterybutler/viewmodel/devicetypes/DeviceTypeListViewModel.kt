package com.chriscartland.batterybutler.viewmodel.devicetypes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationmodels.devicetypes.DeviceTypeGroupOption
import com.chriscartland.batterybutler.presentationmodels.devicetypes.DeviceTypeListUiState
import com.chriscartland.batterybutler.presentationmodels.devicetypes.DeviceTypeSortOption
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject

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
            ) { sort, group, isSortAsc, isGroupAsc ->
                DeviceTypeSortConfig(sort, group, isSortAsc, isGroupAsc)
            },
            getDeviceTypesUseCase(),
        ) { config, list ->
            var sortedList = when (config.sort) {
                DeviceTypeSortOption.NAME -> list.sortedBy { it.name }
                DeviceTypeSortOption.BATTERY_TYPE -> list.sortedWith(compareBy<DeviceType> { it.batteryType ?: "" }.thenBy { it.name })
            }
            if (!config.isSortAsc) {
                sortedList = sortedList.reversed()
            }

            val groupedList = when (config.group) {
                DeviceTypeGroupOption.NONE -> mapOf("All Types" to sortedList)
                DeviceTypeGroupOption.BATTERY_TYPE -> sortedList.groupBy { it.batteryType ?: "Unknown Battery" }
            }

            val finalGroupedList = if (config.group != DeviceTypeGroupOption.NONE) {
                val comparator = if (config.isGroupAsc) naturalOrder<String>() else reverseOrder()
                groupedList.toSortedMap(comparator)
            } else {
                groupedList
            }

            DeviceTypeListUiState.Success(
                groupedTypes = finalGroupedList,
                sortOption = config.sort,
                groupOption = config.group,
                isSortAscending = config.isSortAsc,
                isGroupAscending = config.isGroupAsc,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
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
    val isSortAsc: Boolean,
    val isGroupAsc: Boolean,
)

// Custom implementations
private fun <K : Comparable<K>, V> Map<K, V>.toSortedMap(comparator: Comparator<K>): Map<K, V> =
    this.entries
        .sortedWith(Comparator { a, b -> comparator.compare(a.key, b.key) })
        .associate { it.key to it.value }

private fun <T : Comparable<T>> naturalOrder(): Comparator<T> = Comparator { a, b -> a.compareTo(b) }

private fun <T : Comparable<T>> reverseOrder(): Comparator<T> = Comparator { a, b -> b.compareTo(a) }
