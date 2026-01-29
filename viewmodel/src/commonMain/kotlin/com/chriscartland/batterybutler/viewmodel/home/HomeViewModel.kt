package com.chriscartland.batterybutler.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.SyncStatus
import com.chriscartland.batterybutler.presentationmodel.home.GroupOption
import com.chriscartland.batterybutler.presentationmodel.home.HomeUiState
import com.chriscartland.batterybutler.presentationmodel.home.SortOption
import com.chriscartland.batterybutler.usecase.DismissSyncStatusUseCase
import com.chriscartland.batterybutler.usecase.ExportDataUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.GetDevicesUseCase
import com.chriscartland.batterybutler.usecase.GetSyncStatusUseCase
import com.chriscartland.batterybutler.viewmodel.defaultWhileSubscribed
import com.chriscartland.batterybutler.viewmodel.toSortedMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import kotlin.comparisons.naturalOrder
import kotlin.comparisons.reverseOrder

@Inject
class HomeViewModel(
    private val getDevicesUseCase: GetDevicesUseCase,
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
    private val exportDataUseCase: ExportDataUseCase,
    private val getSyncStatusUseCase: GetSyncStatusUseCase,
    private val dismissSyncStatusUseCase: DismissSyncStatusUseCase,
) : ViewModel() {
    private val sortOptionFlow = MutableStateFlow(SortOption.NAME)
    private val groupOptionFlow = MutableStateFlow(GroupOption.NONE)
    private val isSortAscendingFlow = MutableStateFlow(true)
    private val isGroupAscendingFlow = MutableStateFlow(true)
    private val exportDataFlow = MutableStateFlow<String?>(null)
    private var autoDismissJob: Job? = null

    companion object {
        private const val SYNC_SUCCESS_DISPLAY_DURATION_MS = 2000L
    }

    init {
        // Auto-dismiss Success status after a delay
        viewModelScope.launch {
            getSyncStatusUseCase().collect { status ->
                autoDismissJob?.cancel()
                if (status is SyncStatus.Success) {
                    autoDismissJob = viewModelScope.launch {
                        delay(SYNC_SUCCESS_DISPLAY_DURATION_MS)
                        dismissSyncStatusUseCase()
                    }
                }
            }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        combine(
            sortOptionFlow,
            groupOptionFlow,
            isSortAscendingFlow,
            isGroupAscendingFlow,
            exportDataFlow,
        ) { sort, group, isSortAscending, isGroupAscending, exportData ->
            SortConfig(sort, group, isSortAscending, isGroupAscending, exportData)
        },
        getDevicesUseCase(),
        getDeviceTypesUseCase(),
        getSyncStatusUseCase(),
    ) { config, devices, types, syncStatus ->
        val typeMap = types.associateBy { it.id }

        // Sort
        var sortedDevices = when (config.sort) {
            SortOption.NAME -> devices.sortedBy { it.name }
            SortOption.LOCATION -> devices.sortedWith(compareBy<Device> { it.location ?: "" }.thenBy { it.name })
            SortOption.BATTERY_AGE -> devices.sortedBy { it.batteryLastReplaced }
            SortOption.TYPE -> devices.sortedBy { typeMap[it.typeId]?.name }
        }
        if (!config.isSortAscending) {
            sortedDevices = sortedDevices.reversed()
        }

        // Group
        val groupedDevices = when (config.group) {
            GroupOption.NONE -> mapOf("All Devices" to sortedDevices)
            GroupOption.TYPE -> sortedDevices.groupBy { typeMap[it.typeId]?.name ?: "Unknown" }
            GroupOption.LOCATION -> sortedDevices.groupBy { it.location ?: "Unknown Location" }
        }

        val finalGroupedDevices = if (config.group != GroupOption.NONE) {
            val comparator = if (config.isGroupAscending) naturalOrder<String>() else reverseOrder()
            groupedDevices.toSortedMap(comparator)
        } else {
            groupedDevices
        }

        HomeUiState(
            groupedDevices = finalGroupedDevices,
            deviceTypes = typeMap,
            sortOption = config.sort,
            groupOption = config.group,
            isSortAscending = config.isSortAscending,
            isGroupAscending = config.isGroupAscending,
            exportData = config.exportData,
            syncStatus = syncStatus,
        )
    }.stateIn(
        scope = viewModelScope,
        started = defaultWhileSubscribed(),
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
    val isSortAscending: Boolean,
    val isGroupAscending: Boolean,
    val exportData: String?,
)
