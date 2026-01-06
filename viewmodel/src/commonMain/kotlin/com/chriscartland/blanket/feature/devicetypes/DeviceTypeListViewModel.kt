package com.chriscartland.blanket.feature.devicetypes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.blanket.domain.model.DeviceType
import com.chriscartland.blanket.domain.repository.DeviceRepository
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
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DeviceTypeListUiState.Loading
        )
}

sealed interface DeviceTypeListUiState {
    data object Loading : DeviceTypeListUiState
    data class Success(val deviceTypes: List<DeviceType>) : DeviceTypeListUiState
}
