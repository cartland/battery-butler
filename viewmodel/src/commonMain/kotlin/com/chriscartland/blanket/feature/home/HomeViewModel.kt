package com.chriscartland.blanket.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.blanket.domain.model.Device
import com.chriscartland.blanket.domain.model.DeviceType
import com.chriscartland.blanket.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject

@Inject
class HomeViewModel(
    private val deviceRepository: DeviceRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        deviceRepository.getAllDevices(),
        deviceRepository.getAllDeviceTypes(),
    ) { devices, types ->
        HomeUiState(
            devices = devices,
            deviceTypes = types.associateBy { it.id },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(),
    )
}

data class HomeUiState(
    val devices: List<Device> = emptyList(),
    val deviceTypes: Map<String, DeviceType> = emptyMap(),
)
