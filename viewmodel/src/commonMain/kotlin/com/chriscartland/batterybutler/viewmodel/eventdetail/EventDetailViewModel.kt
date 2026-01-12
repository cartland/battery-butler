package com.chriscartland.batterybutler.viewmodel.eventdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.usecase.DeleteBatteryEventUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceDetailUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.GetEventDetailUseCase
import com.chriscartland.batterybutler.usecase.UpdateBatteryEventUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import me.tatarka.inject.annotations.Inject

import com.chriscartland.batterybutler.presentation.models.eventdetail.EventDetailUiState

@Inject
class EventDetailViewModelFactory(
    private val getEventDetailUseCase: GetEventDetailUseCase,
    private val getDeviceDetailUseCase: GetDeviceDetailUseCase,
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
    private val updateBatteryEventUseCase: UpdateBatteryEventUseCase,
    private val deleteBatteryEventUseCase: DeleteBatteryEventUseCase,
) {
    fun create(eventId: String): EventDetailViewModel =
        EventDetailViewModel(
            eventId,
            getEventDetailUseCase,
            getDeviceDetailUseCase,
            getDeviceTypesUseCase,
            updateBatteryEventUseCase,
            deleteBatteryEventUseCase,
        )
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EventDetailViewModel(
    private val eventId: String,
    private val getEventDetailUseCase: GetEventDetailUseCase,
    private val getDeviceDetailUseCase: GetDeviceDetailUseCase,
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
    private val updateBatteryEventUseCase: UpdateBatteryEventUseCase,
    private val deleteBatteryEventUseCase: DeleteBatteryEventUseCase,
) : ViewModel() {
    val uiState: StateFlow<EventDetailUiState> = getEventDetailUseCase(eventId)
        .flatMapLatest { event ->
            if (event == null) {
                kotlinx.coroutines.flow.flowOf(EventDetailUiState.NotFound)
            } else {
                combine(
                    getDeviceDetailUseCase(event.deviceId).filterNotNull(),
                    getDeviceTypesUseCase(),
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
                updateBatteryEventUseCase(currentState.event.copy(date = newDate))
            }
        }
    }

    fun deleteEvent() {
        viewModelScope.launch {
            deleteBatteryEventUseCase(eventId)
        }
    }
}
