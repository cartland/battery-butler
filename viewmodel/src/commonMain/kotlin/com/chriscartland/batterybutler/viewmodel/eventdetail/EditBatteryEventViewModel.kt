package com.chriscartland.batterybutler.viewmodel.eventdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.presentationmodel.eventdetail.EditBatteryEventScreenState
import com.chriscartland.batterybutler.usecase.DeleteBatteryEventUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceDetailUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.GetEventDetailUseCase
import com.chriscartland.batterybutler.usecase.UpdateBatteryEventUseCase
import com.chriscartland.batterybutler.viewmodel.defaultWhileSubscribed
import com.chriscartland.batterybutler.viewmodel.safeStateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import kotlin.time.Instant

@Inject
class EditBatteryEventViewModelFactory(
    private val getEventDetailUseCase: GetEventDetailUseCase,
    private val getDeviceDetailUseCase: GetDeviceDetailUseCase,
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
    private val updateBatteryEventUseCase: UpdateBatteryEventUseCase,
    private val deleteBatteryEventUseCase: DeleteBatteryEventUseCase,
) {
    fun create(eventId: String): EditBatteryEventViewModel =
        EditBatteryEventViewModel(
            eventId,
            getEventDetailUseCase,
            getDeviceDetailUseCase,
            getDeviceTypesUseCase,
            updateBatteryEventUseCase,
            deleteBatteryEventUseCase,
        )
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EditBatteryEventViewModel(
    private val eventId: String,
    private val getEventDetailUseCase: GetEventDetailUseCase,
    private val getDeviceDetailUseCase: GetDeviceDetailUseCase,
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
    private val updateBatteryEventUseCase: UpdateBatteryEventUseCase,
    private val deleteBatteryEventUseCase: DeleteBatteryEventUseCase,
) : ViewModel() {
    val uiState: StateFlow<EditBatteryEventScreenState> = getEventDetailUseCase(eventId)
        .flatMapLatest { event ->
            if (event == null) {
                flowOf(EditBatteryEventScreenState.NotFound)
            } else {
                combine(
                    getDeviceDetailUseCase(event.deviceId),
                    getDeviceTypesUseCase(),
                ) { device, types ->
                    val deviceType = device?.let { d -> types.find { it.id == d.typeId } }
                    EditBatteryEventScreenState.Success(
                        event = event,
                        device = device,
                        deviceType = deviceType,
                    )
                }
            }
        }.safeStateIn(
            scope = viewModelScope,
            started = defaultWhileSubscribed(),
            initialValue = EditBatteryEventScreenState.Loading,
        )

    fun updateEvent(
        date: Instant,
        batteryType: String?,
        notes: String?,
    ) {
        val currentState = uiState.value
        if (currentState is EditBatteryEventScreenState.Success) {
            viewModelScope.launch {
                updateBatteryEventUseCase(
                    currentState.event.copy(
                        date = date,
                        batteryType = batteryType?.takeIf { it.isNotBlank() },
                        notes = notes?.takeIf { it.isNotBlank() },
                    ),
                )
            }
        }
    }

    fun deleteEvent() {
        viewModelScope.launch {
            deleteBatteryEventUseCase(eventId)
        }
    }
}
