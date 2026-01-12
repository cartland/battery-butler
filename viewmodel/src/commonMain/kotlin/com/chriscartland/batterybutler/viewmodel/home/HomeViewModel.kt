package com.chriscartland.batterybutler.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.usecase.EnsureDefaultDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.ExportDataUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.GetDevicesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

import com.chriscartland.batterybutler.uimodels.home.GroupOption
import com.chriscartland.batterybutler.uimodels.home.HomeUiState
import com.chriscartland.batterybutler.uimodels.home.SortOption

// Custom implementations to avoid JVM-specific dependencies in KMP
private fun <K : Comparable<K>, V> Map<K, V>.toSortedMap(comparator: Comparator<K>): Map<K, V> =
    this.entries
        .sortedWith(Comparator { a, b -> comparator.compare(a.key, b.key) })
        .associate { it.key to it.value }

private fun <T : Comparable<T>> naturalOrder(): Comparator<T> = Comparator { a, b -> a.compareTo(b) }

private fun <T : Comparable<T>> reverseOrder(): Comparator<T> = Comparator { a, b -> b.compareTo(a) }

@Inject
class HomeViewModel(
    private val getDevicesUseCase: GetDevicesUseCase,
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
    private val ensureDefaultDeviceTypesUseCase: EnsureDefaultDeviceTypesUseCase,
    private val exportDataUseCase: ExportDataUseCase,
) : ViewModel() {
    init {
        viewModelScope.launch {
            ensureDefaultDeviceTypesUseCase()
        }
    }

    private val sortOptionFlow = MutableStateFlow(SortOption.NAME)
    private val groupOptionFlow = MutableStateFlow(GroupOption.NONE)
    private val isSortAscendingFlow = MutableStateFlow(true)
    private val isGroupAscendingFlow = MutableStateFlow(true)
    private val exportDataFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        combine(
            sortOptionFlow,
            groupOptionFlow,
            isSortAscendingFlow,
            isGroupAscendingFlow,
            exportDataFlow,
        ) { sort, group, isSortAsc, isGroupAsc, exportData ->
            SortConfig(sort, group, isSortAsc, isGroupAsc, exportData)
        },
        getDevicesUseCase(),
        getDeviceTypesUseCase(),
    ) { config, devices, types ->
        val typeMap = types.associateBy { it.id }

        // Sort
        var sortedDevices = when (config.sort) {
            SortOption.NAME -> devices.sortedBy { it.name }
            SortOption.LOCATION -> devices.sortedWith(compareBy<Device> { it.location ?: "" }.thenBy { it.name })
            SortOption.BATTERY_AGE -> devices.sortedBy { it.batteryLastReplaced }
            SortOption.TYPE -> devices.sortedBy { typeMap[it.typeId]?.name }
        }
        if (!config.isSortAsc) {
            sortedDevices = sortedDevices.reversed()
        }

        // Group
        val groupedDevices = when (config.group) {
            GroupOption.NONE -> mapOf("All Devices" to sortedDevices)
            GroupOption.TYPE -> sortedDevices.groupBy { typeMap[it.typeId]?.name ?: "Unknown" }
            GroupOption.LOCATION -> sortedDevices.groupBy { it.location ?: "Unknown Location" }
            else -> mapOf("All Devices" to sortedDevices)
        }

        val finalGroupedDevices = if (config.group != GroupOption.NONE) {
            val comparator = if (config.isGroupAsc) naturalOrder<String>() else reverseOrder()
            groupedDevices.toSortedMap(comparator)
        } else {
            groupedDevices
        }

        HomeUiState(
            groupedDevices = finalGroupedDevices,
            deviceTypes = typeMap,
            sortOption = config.sort,
            groupOption = config.group,
            isSortAscending = config.isSortAsc,
            isGroupAscending = config.isGroupAsc,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(),
    )

    fun onSortOptionSelected(option: SortOption) {
        sortOptionFlow.value = option
    }

    fun onGroupOptionSelected(option: GroupOption) {
        groupOptionFlow.value = option
    }

    fun toggleSortDirection() {
        isSortAscendingFlow.value = !isSortAscendingFlow.value
    }

    fun toggleGroupDirection() {
        isGroupAscendingFlow.value = !isGroupAscendingFlow.value
    }

    fun onExportData() {
        viewModelScope.launch {
            val json = exportDataUseCase()
            exportDataFlow.value = json
        }
    }

    fun onExportDataConsumed() {
        exportDataFlow.value = null
    }
}

private data class SortConfig(
    val sort: SortOption,
    val group: GroupOption,
    val isSortAsc: Boolean,
    val isGroupAsc: Boolean,
    val exportData: String?,
)
