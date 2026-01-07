package com.chriscartland.batterybutler.feature.adddevicetype

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.DeviceTypeInput
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Inject
class AddDeviceTypeViewModel(
    private val deviceRepository: DeviceRepository,
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
}
