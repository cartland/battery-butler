package com.chriscartland.batterybutler.feature.devicetypes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject

@Inject
class DeviceTypeListViewModel(
    private val deviceRepository: DeviceRepository,
) : ViewModel() {
    val uiState: StateFlow<DeviceTypeListUiState> = deviceRepository
        .getAllDeviceTypes()
        .map { list ->
            DeviceTypeListUiState.Success(list)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DeviceTypeListUiState.Success(emptyList()),
        )
}

sealed interface DeviceTypeListUiState {
    data object Loading : DeviceTypeListUiState

    data class Success(
        val deviceTypes: List<DeviceType>,
    ) : DeviceTypeListUiState
}
