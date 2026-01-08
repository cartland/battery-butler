package com.chriscartland.batterybutler.feature.addbatteryevent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuid4
import com.chriscartland.batterybutler.domain.ai.AiMessage
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.usecase.BatchAddBatteryEventsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import me.tatarka.inject.annotations.Inject

@Inject
class AddBatteryEventViewModel(
    private val deviceRepository: DeviceRepository,
    private val batchAddBatteryEventsUseCase: BatchAddBatteryEventsUseCase,
) : ViewModel() {
    private val _aiMessages = MutableStateFlow<List<AiMessage>>(emptyList())
    val aiMessages: StateFlow<List<AiMessage>> = _aiMessages

    val devices = deviceRepository.getAllDevices().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    fun addEvent(
        deviceId: String,
        date: Instant,
        batteryType: String?,
        notes: String?,
    ) {
        viewModelScope.launch {
            deviceRepository.addEvent(
                BatteryEvent(
                    id = uuid4().toString(),
                    deviceId = deviceId,
                    date = date,
                    batteryType = batteryType,
                    notes = notes,
                ),
            )
            // Need to update device batteryLastReplaced?
            // Domain repo or UseCase should ideally handle this logic consistency.
            // But for manually added single event:
            val device = deviceRepository.getDeviceById(deviceId).first()
            if (device != null && date > device.batteryLastReplaced) {
                deviceRepository.updateDevice(device.copy(batteryLastReplaced = date))
            }
        }
    }

    fun batchAddEvents(input: String) {
        viewModelScope.launch {
            batchAddBatteryEventsUseCase(input).collect { message ->
                _aiMessages.value += message
            }
        }
    }

    fun clearAiMessages() {
        _aiMessages.value = emptyList()
    }
}
