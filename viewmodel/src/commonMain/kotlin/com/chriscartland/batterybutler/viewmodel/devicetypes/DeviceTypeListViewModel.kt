package com.chriscartland.batterybutler.viewmodel.devicetypes

import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.ListArrangement
import com.chriscartland.batterybutler.domain.model.ListScreen
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.repository.DisplayDensityRepository
import com.chriscartland.batterybutler.domain.repository.ListArrangementRepository
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeGroupOption
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeListScreenState
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeSortOption
import com.chriscartland.batterybutler.presentationmodel.home.DensityOption
import com.chriscartland.batterybutler.presentationmodel.home.toDensityOption
import com.chriscartland.batterybutler.presentationmodel.home.toDisplayDensity
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.PreloadCommonTypesUseCase
import com.chriscartland.batterybutler.usecase.ResyncUseCase
import com.chriscartland.batterybutler.viewmodel.defaultWhileSubscribed
import com.chriscartland.batterybutler.viewmodel.retryableStateIn
import com.chriscartland.batterybutler.viewmodel.util.sortAndGroup
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow as ObservableMutableStateFlow

@Inject
class DeviceTypeListViewModel(
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
    private val preloadCommonTypesUseCase: PreloadCommonTypesUseCase,
    private val resyncUseCase: ResyncUseCase,
    private val displayDensityRepository: DisplayDensityRepository,
    private val listArrangementRepository: ListArrangementRepository,
) : ViewModel() {
    // Exposed to SwiftUI: must use the observable factory so @StateViewModel re-renders.
    // (Private funnel flows below stay plain — they combine into the observable uiState.)
    private val _actionError = ObservableMutableStateFlow<String?>(viewModelScope, null)
    val actionError: StateFlow<String?> = _actionError

    private val _isRefreshing = ObservableMutableStateFlow(viewModelScope, false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /**
     * Pull-to-refresh trigger. Deliberately independent of the shared sync status (which the
     * background sync loop also mutates on its own timer) so the refresh spinner only reflects
     * this specific user-initiated action.
     */
    fun onRefresh() {
        viewModelScope.coroutineScope.launch {
            _isRefreshing.value = true
            resyncUseCase()
            _isRefreshing.value = false
        }
    }

    fun dismissActionError() {
        _actionError.value = null
    }

    // Persisted per list screen, so this survives app restarts and is independent of the device
    // list's own arrangement -- sorting devices by battery age says nothing about device types.
    private val arrangementFlow = listArrangementRepository.arrangement(ListScreen.DEVICE_TYPES)

    // Shared app-wide with the Home device list -- same stored preference, same DataStore key,
    // so toggling density on either screen moves both.
    private val densityOptionFlow = displayDensityRepository.displayDensity.map { it.toDensityOption() }

    fun onDensityOptionSelected(option: DensityOption) {
        viewModelScope.coroutineScope.launch {
            displayDensityRepository.setDisplayDensity(option.toDisplayDensity())
        }
    }

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
                    arrangementFlow,
                    densityOptionFlow,
                ) { arrangement, density ->
                    DeviceTypeSortConfig(
                        sort = arrangement.sortKey.toDeviceTypeSortOption(),
                        group = arrangement.groupKey.toDeviceTypeGroupOption(),
                        isSortAscending = arrangement.isSortAscending ?: DEFAULT_TYPE_SORT_ASCENDING,
                        isGroupAscending = arrangement.isGroupAscending ?: DEFAULT_TYPE_GROUP_ASCENDING,
                        density = density,
                    )
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
                    densityOption = config.density,
                )
            }
        },
    )

    fun onSortOptionSelected(option: DeviceTypeSortOption) {
        persist { it.copy(sortKey = option.storageKey()) }
    }

    fun onGroupOptionSelected(option: DeviceTypeGroupOption) {
        persist { it.copy(groupKey = option.storageKey()) }
    }

    fun toggleSortDirection() {
        persist { it.copy(isSortAscending = !(it.isSortAscending ?: DEFAULT_TYPE_SORT_ASCENDING)) }
    }

    fun toggleGroupDirection() {
        persist { it.copy(isGroupAscending = !(it.isGroupAscending ?: DEFAULT_TYPE_GROUP_ASCENDING)) }
    }

    /** Reads from the repository, not [uiState], for the reason given on HomeViewModel.persist. */
    private fun persist(change: (ListArrangement) -> ListArrangement) {
        viewModelScope.coroutineScope.launch {
            val current = arrangementFlow.first()
            listArrangementRepository.setArrangement(ListScreen.DEVICE_TYPES, change(current))
        }
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
    val density: DensityOption,
)

/** Defaults matching the behaviour before these choices were persisted. */
private const val DEFAULT_TYPE_SORT_ASCENDING = true
private const val DEFAULT_TYPE_GROUP_ASCENDING = true

// Lowercase tokens, not `name`/`ordinal` -- see the note in HomeViewModel.
private const val TYPE_SORT_NAME = "name"
private const val TYPE_SORT_BATTERY_TYPE = "battery_type"
private const val TYPE_GROUP_NONE = "none"
private const val TYPE_GROUP_BATTERY_TYPE = "battery_type"

private fun DeviceTypeSortOption.storageKey(): String =
    when (this) {
        DeviceTypeSortOption.NAME -> TYPE_SORT_NAME
        DeviceTypeSortOption.BATTERY_TYPE -> TYPE_SORT_BATTERY_TYPE
    }

private fun String?.toDeviceTypeSortOption(): DeviceTypeSortOption =
    when (this) {
        TYPE_SORT_BATTERY_TYPE -> DeviceTypeSortOption.BATTERY_TYPE
        else -> DeviceTypeSortOption.NAME
    }

private fun DeviceTypeGroupOption.storageKey(): String =
    when (this) {
        DeviceTypeGroupOption.NONE -> TYPE_GROUP_NONE
        DeviceTypeGroupOption.BATTERY_TYPE -> TYPE_GROUP_BATTERY_TYPE
    }

private fun String?.toDeviceTypeGroupOption(): DeviceTypeGroupOption =
    when (this) {
        TYPE_GROUP_BATTERY_TYPE -> DeviceTypeGroupOption.BATTERY_TYPE
        else -> DeviceTypeGroupOption.NONE
    }
