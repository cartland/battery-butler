package com.chriscartland.blanket.feature.devicedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.blanket.domain.model.BatteryEvent
import com.chriscartland.blanket.domain.model.Device
import com.chriscartland.blanket.domain.model.DeviceType
import com.chriscartland.blanket.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import me.tatarka.inject.annotations.Inject
import com.benasher44.uuid.uuid4

@Inject
class DeviceDetailViewModelFactory(
    private val deviceRepository: DeviceRepository
) {
    fun create(deviceId: String): DeviceDetailViewModel {
        return DeviceDetailViewModel(deviceId, deviceRepository)
    }
}

class DeviceDetailViewModel(
    private val deviceId: String,
    private val deviceRepository: DeviceRepository,
) : ViewModel() {

    val uiState: StateFlow<DeviceDetailUiState> = combine(
        deviceRepository.getDeviceById(deviceId),
        deviceRepository.getAllDeviceTypes(),
        deviceRepository.getEventsForDevice(deviceId)
    ) { device, types, events ->
        if (device == null) {
            DeviceDetailUiState.NotFound
        } else {
            val deviceType = types.find { it.id == device.typeId }
            DeviceDetailUiState.Success(
                device = device,
                deviceType = deviceType,
                events = events
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DeviceDetailUiState.Loading
    )

    fun recordReplacement() {
        viewModelScope.launch {
            val event = BatteryEvent(
                id = uuid4().toString(),
                deviceId = deviceId,
                date = Clock.System.now()
            )
            deviceRepository.addEvent(event)
            // Also update the device's last replaced date for quick access
            val currentDevice = (uiState.value as? DeviceDetailUiState.Success)?.device
            if (currentDevice != null) {
                 deviceRepository.updateDevice(currentDevice.copy(batteryLastReplaced = event.date))
            }
        }
    }
}

sealed interface DeviceDetailUiState {
    data object Loading : DeviceDetailUiState
    data object NotFound : DeviceDetailUiState
    data class Success(
        val device: Device,
        val deviceType: DeviceType?,
        val events: List<BatteryEvent>
    ) : DeviceDetailUiState
}
