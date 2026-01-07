package com.chriscartland.blanket.feature.adddevice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.blanket.domain.model.Device
import com.chriscartland.blanket.domain.model.DeviceType
import com.chriscartland.blanket.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import me.tatarka.inject.annotations.Inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Inject
class AddDeviceViewModel(
    private val deviceRepository: DeviceRepository,
) : ViewModel() {
    val deviceTypes: StateFlow<List<DeviceType>> = deviceRepository
        .getAllDeviceTypes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    @OptIn(ExperimentalUuidApi::class)
    fun addDevice(
        name: String,
        typeId: String,
    ) {
        viewModelScope.launch {
            val newDevice = Device(
                id = Uuid.random().toString(),
                name = name,
                typeId = typeId,
                batteryLastReplaced = Clock.System.now(),
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
}
