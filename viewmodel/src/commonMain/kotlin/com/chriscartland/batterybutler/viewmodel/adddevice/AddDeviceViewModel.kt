package com.chriscartland.batterybutler.viewmodel.adddevice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.BatchOperationResult
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceInput
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.usecase.AddDeviceUseCase
import com.chriscartland.batterybutler.usecase.BatchAddDevicesUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.viewmodel.defaultWhileSubscribed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Inject
class AddDeviceViewModel(
    private val addDeviceUseCase: AddDeviceUseCase,
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
    private val batchAddDevicesUseCase: BatchAddDevicesUseCase,
) : ViewModel() {
    val deviceTypes: StateFlow<List<DeviceType>> = getDeviceTypesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = defaultWhileSubscribed(),
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
            addDeviceUseCase(newDevice)
        }
    }

    private val _aiMessages = MutableStateFlow<List<BatchOperationResult>>(emptyList())
    val aiMessages: StateFlow<List<BatchOperationResult>> = _aiMessages

    fun batchAddDevices(input: String) {
        viewModelScope.launch {
            batchAddDevicesUseCase(input).collect { message ->
                _aiMessages.update { it + message }
            }
        }
    }

    fun clearAiMessages() {
        _aiMessages.value = emptyList()
    }
}
