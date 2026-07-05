package com.chriscartland.batterybutler.viewmodel.history

import com.chriscartland.batterybutler.presentationmodel.history.HistoryItemModel
import com.chriscartland.batterybutler.presentationmodel.history.HistoryListScreenState
import com.chriscartland.batterybutler.usecase.GetBatteryEventsUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.GetDevicesUseCase
import com.chriscartland.batterybutler.usecase.ResyncUseCase
import com.chriscartland.batterybutler.viewmodel.defaultWhileSubscribed
import com.chriscartland.batterybutler.viewmodel.retryableStateIn
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow as ObservableMutableStateFlow

@Inject
class HistoryListViewModel(
    private val getBatteryEventsUseCase: GetBatteryEventsUseCase,
    private val getDevicesUseCase: GetDevicesUseCase,
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
    private val resyncUseCase: ResyncUseCase,
) : ViewModel() {
    private val retryTrigger = MutableStateFlow(0)

    fun retry() {
        retryTrigger.update { it + 1 }
    }

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

    val uiState: StateFlow<HistoryListScreenState> = retryableStateIn(
        viewModelScope = viewModelScope,
        retryTrigger = retryTrigger,
        started = defaultWhileSubscribed(),
        initialValue = HistoryListScreenState.Loading,
        onError = { HistoryListScreenState.Error(it.message ?: "Failed to load history") },
        source = {
            combine(
                getBatteryEventsUseCase(),
                getDevicesUseCase(),
                getDeviceTypesUseCase(),
            ) { events, devices, types ->
                val deviceMap = devices.associateBy { it.id }
                val typeMap = types.associateBy { it.id }

                val items = events.map { event ->
                    val device = deviceMap[event.deviceId]
                    val type = device?.let { typeMap[it.typeId] }
                    HistoryItemModel(
                        event = event,
                        deviceName = device?.name ?: "Unknown Device",
                        deviceTypeName = type?.name ?: "Unknown Type",
                        deviceLocation = device?.location,
                    )
                }
                HistoryListScreenState.Success(items)
            }
        },
    )
}
