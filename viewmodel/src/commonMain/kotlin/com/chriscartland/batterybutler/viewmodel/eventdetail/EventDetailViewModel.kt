package com.chriscartland.batterybutler.viewmodel.eventdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.presentationmodel.eventdetail.EventDetailUiState
import com.chriscartland.batterybutler.usecase.GetDeviceDetailUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.GetEventDetailUseCase
import com.chriscartland.batterybutler.viewmodel.defaultWhileSubscribed
import com.chriscartland.batterybutler.viewmodel.safeStateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import me.tatarka.inject.annotations.Inject

@Inject
class EventDetailViewModelFactory(
    private val getEventDetailUseCase: GetEventDetailUseCase,
    private val getDeviceDetailUseCase: GetDeviceDetailUseCase,
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
) {
    fun create(eventId: String): EventDetailViewModel =
        EventDetailViewModel(
            eventId,
            getEventDetailUseCase,
            getDeviceDetailUseCase,
            getDeviceTypesUseCase,
        )
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EventDetailViewModel(
    eventId: String,
    getEventDetailUseCase: GetEventDetailUseCase,
    getDeviceDetailUseCase: GetDeviceDetailUseCase,
    getDeviceTypesUseCase: GetDeviceTypesUseCase,
) : ViewModel() {
    val uiState: StateFlow<EventDetailUiState> = getEventDetailUseCase(eventId)
        .flatMapLatest { event ->
            if (event == null) {
                flowOf(EventDetailUiState.NotFound)
            } else {
                combine(
                    getDeviceDetailUseCase(event.deviceId),
                    getDeviceTypesUseCase(),
                ) { device, types ->
                    val deviceType = device?.let { d -> types.find { it.id == d.typeId } }
                    EventDetailUiState.Success(
                        event = event,
                        device = device,
                        deviceType = deviceType,
                    )
                }
            }
        }.safeStateIn(
            scope = viewModelScope,
            started = defaultWhileSubscribed(),
            initialValue = EventDetailUiState.Loading,
        )
}
