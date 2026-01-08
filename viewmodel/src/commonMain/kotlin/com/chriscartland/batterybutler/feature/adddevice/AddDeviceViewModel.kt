package com.chriscartland.batterybutler.feature.adddevice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceInput
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.usecase.BatchAddDevicesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.tatarka.inject.annotations.Inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Inject
class AddDeviceViewModel(
    private val deviceRepository: DeviceRepository,
    private val batchAddDevicesUseCase: BatchAddDevicesUseCase,
) : ViewModel() {
    val deviceTypes: StateFlow<List<DeviceType>> = deviceRepository
        .getAllDeviceTypes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    @OptIn(ExperimentalUuidApi::class)
    fun addDevice(input: DeviceInput) {
        viewModelScope.launch {
            val newDevice = Device(
                id = Uuid.random().toString(),
                name = input.name,
                location = input.location,
                typeId = input.typeId,
                imagePath = input.imagePath,
                batteryLastReplaced = Instant.fromEpochMilliseconds(0),
                lastUpdated = Clock.System.now(),
            )
            deviceRepository.addDevice(newDevice)
        }
    }

    // Helper to seed types if empty (temporary for testing)
    fun seedDeviceTypes() {
        viewModelScope.launch {
            deviceRepository.ensureDefaultDeviceTypes()
        }
    }

    private val _aiMessages = kotlinx.coroutines.flow.MutableStateFlow<List<com.chriscartland.batterybutler.domain.ai.AiMessage>>(
        emptyList(),
    )
    val aiMessages: StateFlow<List<com.chriscartland.batterybutler.domain.ai.AiMessage>> = _aiMessages

    fun batchAddDevices(input: String) {
        viewModelScope.launch {
            batchAddDevicesUseCase(input).collect { message ->
                _aiMessages.value += message
            }
        }
    }

    fun clearAiMessages() {
        _aiMessages.value = emptyList()
    }
}
