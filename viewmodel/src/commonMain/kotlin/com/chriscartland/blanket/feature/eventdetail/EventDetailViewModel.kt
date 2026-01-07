package com.chriscartland.blanket.feature.eventdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.blanket.domain.model.BatteryEvent
import com.chriscartland.blanket.domain.model.Device
import com.chriscartland.blanket.domain.model.DeviceType
import com.chriscartland.blanket.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import me.tatarka.inject.annotations.Inject

@Inject
class EventDetailViewModelFactory(
    private val deviceRepository: DeviceRepository,
) {
    fun create(eventId: String): EventDetailViewModel = EventDetailViewModel(eventId, deviceRepository)
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EventDetailViewModel(
    private val eventId: String,
    private val deviceRepository: DeviceRepository,
) : ViewModel() {
    val uiState: StateFlow<EventDetailUiState> = deviceRepository
        .getEventById(eventId)
        .flatMapLatest { event ->
            if (event == null) {
                kotlinx.coroutines.flow.flowOf(EventDetailUiState.NotFound)
            } else {
                combine(
                    deviceRepository.getDeviceById(event.deviceId).filterNotNull(),
                    deviceRepository.getAllDeviceTypes(),
                ) { device, types ->
                    val deviceType = types.find { it.id == device.typeId }
                    EventDetailUiState.Success(
                        event = event,
                        device = device,
                        deviceType = deviceType,
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = EventDetailUiState.Loading,
        )

    fun updateDate(newDate: Instant) {
        val currentState = uiState.value
        if (currentState is EventDetailUiState.Success) {
            viewModelScope.launch {
                deviceRepository.updateEvent(currentState.event.copy(date = newDate))
            }
        }
    }

    fun deleteEvent() {
        viewModelScope.launch {
            deviceRepository.deleteEvent(eventId)
        }
    }
}

sealed interface EventDetailUiState {
    data object Loading : EventDetailUiState

    data object NotFound : EventDetailUiState

    data class Success(
        val event: BatteryEvent,
        val device: Device,
        val deviceType: DeviceType?,
    ) : EventDetailUiState
}
