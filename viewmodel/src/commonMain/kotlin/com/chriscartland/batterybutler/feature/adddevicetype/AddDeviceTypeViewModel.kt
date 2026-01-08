package com.chriscartland.batterybutler.feature.adddevicetype

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.DeviceTypeInput
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

import com.chriscartland.batterybutler.usecase.BatchAddDeviceTypesUseCase

@Inject
class AddDeviceTypeViewModel(
    private val deviceRepository: DeviceRepository,
    private val batchAddDeviceTypesUseCase: BatchAddDeviceTypesUseCase,
) : ViewModel() {
    @OptIn(ExperimentalUuidApi::class)
    fun addDeviceType(input: DeviceTypeInput) {
        viewModelScope.launch {
            val newType = DeviceType(
                id = Uuid.random().toString(),
                name = input.name,
                defaultIcon = input.defaultIcon,
                batteryType = input.batteryType,
                batteryQuantity = input.batteryQuantity,
            )
            deviceRepository.addDeviceType(newType)
        }
    }

    private val _aiMessages = kotlinx.coroutines.flow.MutableStateFlow<List<com.chriscartland.batterybutler.domain.ai.AiMessage>>(emptyList())
    val aiMessages: StateFlow<List<com.chriscartland.batterybutler.domain.ai.AiMessage>> = _aiMessages

    fun batchAddDeviceTypes(input: String) {
        viewModelScope.launch {
            batchAddDeviceTypesUseCase(input).collect { message ->
                _aiMessages.value += message
            }
        }
    }

    fun clearAiMessages() {
        _aiMessages.value = emptyList()
    }
}
