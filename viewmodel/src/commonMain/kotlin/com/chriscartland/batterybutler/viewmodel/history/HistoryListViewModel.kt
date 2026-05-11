package com.chriscartland.batterybutler.viewmodel.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.presentationmodel.history.HistoryItemModel
import com.chriscartland.batterybutler.presentationmodel.history.HistoryListScreenState
import com.chriscartland.batterybutler.usecase.GetBatteryEventsUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.GetDevicesUseCase
import com.chriscartland.batterybutler.viewmodel.defaultWhileSubscribed
import com.chriscartland.batterybutler.viewmodel.retryableStateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import me.tatarka.inject.annotations.Inject

@Inject
class HistoryListViewModel(
    private val getBatteryEventsUseCase: GetBatteryEventsUseCase,
    private val getDevicesUseCase: GetDevicesUseCase,
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
) : ViewModel() {
    private val retryTrigger = MutableStateFlow(0)

    fun retry() {
        retryTrigger.update { it + 1 }
    }

    val uiState: StateFlow<HistoryListScreenState> = retryableStateIn(
        scope = viewModelScope,
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
