package com.chriscartland.batterybutler.viewmodel.home

import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.SyncStatus
import com.chriscartland.batterybutler.domain.repository.DisplayDensityRepository
import com.chriscartland.batterybutler.presentationmodel.home.DensityOption
import com.chriscartland.batterybutler.presentationmodel.home.GroupOption
import com.chriscartland.batterybutler.presentationmodel.home.HomeScreenState
import com.chriscartland.batterybutler.presentationmodel.home.SortOption
import com.chriscartland.batterybutler.presentationmodel.home.toDensityOption
import com.chriscartland.batterybutler.presentationmodel.home.toDisplayDensity
import com.chriscartland.batterybutler.usecase.DismissSyncStatusUseCase
import com.chriscartland.batterybutler.usecase.ExportDataUseCase
import com.chriscartland.batterybutler.usecase.GetCachedDeviceImageUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.GetDevicesUseCase
import com.chriscartland.batterybutler.usecase.GetSyncStatusUseCase
import com.chriscartland.batterybutler.usecase.ResyncUseCase
import com.chriscartland.batterybutler.viewmodel.defaultWhileSubscribed
import com.chriscartland.batterybutler.viewmodel.retryableStateIn
import com.chriscartland.batterybutler.viewmodel.util.sortAndGroup
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow as ObservableMutableStateFlow

@OptIn(ExperimentalCoroutinesApi::class)
@Inject
class HomeViewModel(
    private val getDevicesUseCase: GetDevicesUseCase,
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
    private val exportDataUseCase: ExportDataUseCase,
    private val getSyncStatusUseCase: GetSyncStatusUseCase,
    private val dismissSyncStatusUseCase: DismissSyncStatusUseCase,
    private val resyncUseCase: ResyncUseCase,
    private val getCachedDeviceImageUseCase: GetCachedDeviceImageUseCase,
    private val displayDensityRepository: DisplayDensityRepository,
) : ViewModel() {
    private val sortOptionFlow = MutableStateFlow(SortOption.BATTERY_AGE)
    private val groupOptionFlow = MutableStateFlow(GroupOption.NONE)
    private val isSortAscendingFlow = MutableStateFlow(false)
    private val isGroupAscendingFlow = MutableStateFlow(true)

    // Persisted app-wide, unlike sort/group which stay session-only. Reads the stored
    // DisplayDensity (which may be UNSPECIFIED on a fresh install) and resolves it to the
    // renderable two-case DensityOption.
    private val densityOptionFlow = displayDensityRepository.displayDensity.map { it.toDensityOption() }
    private val exportDataFlow = MutableStateFlow<String?>(null)
    private var autoDismissJob: Job? = null

    /** Last images map emitted, used to seed re-keyed image observations — see the uiState liveness guard. */
    private var lastKnownImages: Map<String, DeviceImageBytes> = emptyMap()

    companion object {
        private const val SYNC_SUCCESS_DISPLAY_DURATION_MS = 2000L
    }

    init {
        // Auto-dismiss Success status after a delay
        viewModelScope.coroutineScope.launch {
            getSyncStatusUseCase().collect { status ->
                autoDismissJob?.cancel()
                if (status is SyncStatus.Success) {
                    autoDismissJob = viewModelScope.coroutineScope.launch {
                        delay(SYNC_SUCCESS_DISPLAY_DURATION_MS)
                        dismissSyncStatusUseCase()
                    }
                }
            }
        }
    }

    private val retryTrigger = MutableStateFlow(0)

    fun retry() {
        retryTrigger.update { it + 1 }
    }

    val uiState: StateFlow<HomeScreenState> = retryableStateIn(
        viewModelScope = viewModelScope,
        retryTrigger = retryTrigger,
        started = defaultWhileSubscribed(),
        initialValue = HomeScreenState(),
        onError = { HomeScreenState(error = it.message ?: "Failed to load devices") },
        source = {
            // `combine`'s typed overloads top out at five flows, so the five list-display options
            // fill the inner combine and `exportDataFlow` (unrelated to display) rides on the outer.
            combine(
                combine(
                    sortOptionFlow,
                    groupOptionFlow,
                    isSortAscendingFlow,
                    isGroupAscendingFlow,
                    densityOptionFlow,
                ) { sort, group, isSortAscending, isGroupAscending, density ->
                    DisplayConfig(sort, group, isSortAscending, isGroupAscending, density)
                },
                getDevicesUseCase(),
                getDeviceTypesUseCase(),
                getSyncStatusUseCase(),
                exportDataFlow,
            ) { config, devices, types, syncStatus, exportData ->
                DeviceListInputs(config, devices, types, syncStatus, exportData)
            }.flatMapLatest { inputs ->
                // Liveness guard: the image observation's first value comes from real DB queries
                // (one Room flow per etag combined), so without a seed the WHOLE uiState would
                // withhold its first emission — an already-populated device list stuck behind
                // image hydration (loading screen forever if any image query stalls). Seed with
                // the last known map (empty on first load); etags are content-addressed, so a
                // briefly-stale entry is harmless and is replaced by the real emission.
                observeImagesByEtag(inputs.devices)
                    .onStart { emit(lastKnownImages) }
                    .onEach { lastKnownImages = it }
                    .map { images -> inputs to images }
            }.map { (inputs, images) ->
                val (config, devices, types, syncStatus, exportData) = inputs
                val typeMap = types.associateBy { it.id }

                val sortComparator = when (config.sort) {
                    SortOption.NAME -> compareBy<Device> { it.name }
                    SortOption.LOCATION -> compareBy<Device> { it.location ?: "" }.thenBy { it.name }
                    SortOption.BATTERY_AGE -> compareBy { it.batteryLastReplaced }
                    SortOption.TYPE -> compareBy { typeMap[it.typeId]?.name ?: "" }
                }

                val groupKeySelector = when (config.group) {
                    GroupOption.NONE -> null
                    GroupOption.TYPE -> { device: Device -> typeMap[device.typeId]?.name ?: "Unknown" }
                    GroupOption.LOCATION -> { device: Device -> device.location ?: "Unknown Location" }
                }

                val finalGroupedDevices = sortAndGroup(
                    items = devices,
                    sortComparator = sortComparator,
                    isSortAscending = config.isSortAscending,
                    groupKeySelector = groupKeySelector,
                    defaultGroupName = "All Devices",
                    isGroupAscending = config.isGroupAscending,
                )

                HomeScreenState(
                    groupedDevices = finalGroupedDevices,
                    deviceTypes = typeMap,
                    sortOption = config.sort,
                    groupOption = config.group,
                    isSortAscending = config.isSortAscending,
                    isGroupAscending = config.isGroupAscending,
                    exportData = exportData,
                    syncStatus = syncStatus,
                    deviceImagesByEtag = images,
                    densityOption = config.density,
                )
            }
        },
    )

    fun onSortOptionSelected(option: SortOption) {
        sortOptionFlow.value = option
    }

    fun onGroupOptionSelected(option: GroupOption) {
        groupOptionFlow.value = option
    }

    fun onDensityOptionSelected(option: DensityOption) {
        viewModelScope.coroutineScope.launch {
            displayDensityRepository.setDisplayDensity(option.toDisplayDensity())
        }
    }

    fun toggleSortDirection() {
        isSortAscendingFlow.value = !isSortAscendingFlow.value
    }

    fun toggleGroupDirection() {
        isGroupAscendingFlow.value = !isGroupAscendingFlow.value
    }

    fun onExportData() {
        viewModelScope.coroutineScope.launch {
            val json = exportDataUseCase()
            exportDataFlow.value = json
        }
    }

    fun onExportDataConsumed() {
        exportDataFlow.value = null
    }

    // Exposed to SwiftUI: must use the observable factory so @StateViewModel re-renders.
    private val _isRefreshing = ObservableMutableStateFlow(viewModelScope, false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /**
     * Pull-to-refresh trigger. Deliberately independent of [SyncStatus] (which the background
     * sync loop also mutates on its own timer) so the refresh spinner only reflects this
     * specific user-initiated action, not unrelated background sync activity.
     */
    fun onRefresh() {
        viewModelScope.coroutineScope.launch {
            _isRefreshing.value = true
            resyncUseCase()
            _isRefreshing.value = false
        }
    }

    /**
     * Reactive map of every distinct [Device.imageEtag] among [devices] to its cached bytes,
     * re-keyed (via the caller's `flatMapLatest`) whenever the device list's set of etags
     * changes. An etag missing from the result just means "not cached yet" -- the list item
     * falls back to its icon until sync populates it. See `docs/DEVICE_IMAGES.md` §6D.
     */
    private fun observeImagesByEtag(devices: List<Device>): Flow<Map<String, DeviceImageBytes>> {
        val etags = devices.mapNotNull { it.imageEtag }.distinct()
        if (etags.isEmpty()) return flowOf(emptyMap())
        return combine(etags.map { etag -> getCachedDeviceImageUseCase(etag).map { bytes -> etag to bytes } }) { pairs ->
            pairs.mapNotNull { (etag, bytes) -> bytes?.let { etag to it } }.toMap()
        }
    }
}

private data class DisplayConfig(
    val sort: SortOption,
    val group: GroupOption,
    val isSortAscending: Boolean,
    val isGroupAscending: Boolean,
    val density: DensityOption,
)

private data class DeviceListInputs(
    val config: DisplayConfig,
    val devices: List<Device>,
    val types: List<DeviceType>,
    val syncStatus: SyncStatus,
    val exportData: String?,
)
