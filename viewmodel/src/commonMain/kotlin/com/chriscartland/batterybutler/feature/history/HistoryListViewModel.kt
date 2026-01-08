package com.chriscartland.batterybutler.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject

sealed interface HistoryListUiState {
    data object Loading : HistoryListUiState

    data class Success(
        val items: List<HistoryItemUiModel>,
    ) : HistoryListUiState
}

data class HistoryItemUiModel(
    val event: BatteryEvent,
    val deviceName: String,
    val deviceTypeName: String,
    val deviceLocation: String?,
)

@Inject
class HistoryListViewModel(
    private val deviceRepository: DeviceRepository,
) : ViewModel() {
    val uiState: StateFlow<HistoryListUiState> = combine(
        deviceRepository.getAllEvents(),
        deviceRepository.getAllDevices(),
        deviceRepository.getAllDeviceTypes(),
    ) { events, devices, types ->
        val deviceMap = devices.associateBy { it.id }
        val typeMap = types.associateBy { it.id }

        val items = events.map { event ->
            val device = deviceMap[event.deviceId]
            val type = device?.let { typeMap[it.typeId] }
            HistoryItemUiModel(
                event = event,
                deviceName = device?.name ?: "Unknown Device",
                deviceTypeName = type?.name ?: "Unknown Type",
                deviceLocation = device?.location,
            )
        }
        HistoryListUiState.Success(items)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryListUiState.Loading,
    )
}
