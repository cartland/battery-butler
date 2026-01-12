package com.chriscartland.batterybutler.viewmodel.devicedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuid4
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.presentation.models.devicedetail.DeviceDetailUiState
import com.chriscartland.batterybutler.usecase.AddBatteryEventUseCase
import com.chriscartland.batterybutler.usecase.GetBatteryEventsUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceDetailUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.UpdateDeviceUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import me.tatarka.inject.annotations.Inject

@Inject
class DeviceDetailViewModelFactory(
    private val getDeviceDetailUseCase: GetDeviceDetailUseCase,
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
    private val getBatteryEventsUseCase: GetBatteryEventsUseCase,
    private val addBatteryEventUseCase: AddBatteryEventUseCase,
    private val updateDeviceUseCase: UpdateDeviceUseCase,
) {
    fun create(deviceId: String): DeviceDetailViewModel =
        DeviceDetailViewModel(
            deviceId,
            getDeviceDetailUseCase,
            getDeviceTypesUseCase,
            getBatteryEventsUseCase,
            addBatteryEventUseCase,
            updateDeviceUseCase,
        )
}

class DeviceDetailViewModel(
    private val deviceId: String,
    private val getDeviceDetailUseCase: GetDeviceDetailUseCase,
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
    private val getBatteryEventsUseCase: GetBatteryEventsUseCase,
    private val addBatteryEventUseCase: AddBatteryEventUseCase,
    private val updateDeviceUseCase: UpdateDeviceUseCase,
) : ViewModel() {
    val uiState: StateFlow<DeviceDetailUiState> = combine(
        getDeviceDetailUseCase(deviceId),
        getDeviceTypesUseCase(),
        getBatteryEventsUseCase.forDevice(deviceId),
    ) { device, types, events ->
        if (device == null) {
            DeviceDetailUiState.NotFound
        } else {
            val deviceType = types.find { it.id == device.typeId }
            DeviceDetailUiState.Success(
                device = device,
                deviceType = deviceType,
                events = events,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DeviceDetailUiState.Loading,
    )

    fun recordReplacement() {
        viewModelScope.launch {
            val event = BatteryEvent(
                id = uuid4().toString(),
                deviceId = deviceId,
                date = Clock.System.now(),
            )
            addBatteryEventUseCase(event)
            // Also update the device's last replaced date for quick access
            val currentDevice = (uiState.value as? DeviceDetailUiState.Success)?.device
            if (currentDevice != null) {
                updateDeviceUseCase(currentDevice.copy(batteryLastReplaced = event.date))
            }
        }
    }
}
