package com.chriscartland.batterybutler.viewmodel.eventdetail

import com.chriscartland.batterybutler.presentationmodel.eventdetail.EventDetailScreenState
import com.chriscartland.batterybutler.usecase.DeleteBatteryEventUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceDetailUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.GetEventDetailUseCase
import com.chriscartland.batterybutler.viewmodel.defaultWhileSubscribed
import com.chriscartland.batterybutler.viewmodel.safeStateIn
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@Inject
class EventDetailViewModelFactory(
    private val getEventDetailUseCase: GetEventDetailUseCase,
    private val getDeviceDetailUseCase: GetDeviceDetailUseCase,
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
    private val deleteBatteryEventUseCase: DeleteBatteryEventUseCase,
) {
    fun create(eventId: String): EventDetailViewModel =
        EventDetailViewModel(
            eventId,
            getEventDetailUseCase,
            getDeviceDetailUseCase,
            getDeviceTypesUseCase,
            deleteBatteryEventUseCase,
        )
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EventDetailViewModel(
    private val eventId: String,
    getEventDetailUseCase: GetEventDetailUseCase,
    getDeviceDetailUseCase: GetDeviceDetailUseCase,
    getDeviceTypesUseCase: GetDeviceTypesUseCase,
    private val deleteBatteryEventUseCase: DeleteBatteryEventUseCase,
) : ViewModel() {
    val uiState: StateFlow<EventDetailScreenState> = getEventDetailUseCase(eventId)
        .flatMapLatest { event ->
            if (event == null) {
                flowOf(EventDetailScreenState.NotFound)
            } else {
                combine(
                    getDeviceDetailUseCase(event.deviceId),
                    getDeviceTypesUseCase(),
                ) { device, types ->
                    val deviceType = device?.let { d -> types.find { it.id == d.typeId } }
                    EventDetailScreenState.Success(
                        event = event,
                        device = device,
                        deviceType = deviceType,
                    )
                }
            }
        }.safeStateIn(
            viewModelScope = viewModelScope,
            started = defaultWhileSubscribed(),
            initialValue = EventDetailScreenState.Loading,
        )

    fun deleteEvent() {
        viewModelScope.coroutineScope.launch {
            deleteBatteryEventUseCase(eventId)
        }
    }
}
