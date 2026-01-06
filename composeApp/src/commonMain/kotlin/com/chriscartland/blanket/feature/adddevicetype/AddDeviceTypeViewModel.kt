package com.chriscartland.blanket.feature.adddevicetype

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.blanket.domain.model.DeviceType
import com.chriscartland.blanket.domain.repository.DeviceRepository
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Inject
class AddDeviceTypeViewModel(
    private val deviceRepository: DeviceRepository,
) : ViewModel() {

    @OptIn(ExperimentalUuidApi::class)
    fun addDeviceType(
        name: String, 
        icon: String?, 
        batteryType: String, 
        batteryQuantity: Int
    ) {
        viewModelScope.launch {
            val newType = DeviceType(
                id = Uuid.random().toString(),
                name = name,
                defaultIcon = icon,
                batteryType = batteryType,
                batteryQuantity = batteryQuantity
            )
            deviceRepository.addDeviceType(newType)
        }
    }
}
