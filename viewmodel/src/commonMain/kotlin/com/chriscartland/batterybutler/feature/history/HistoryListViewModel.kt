package com.chriscartland.batterybutler.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject

sealed interface HistoryListUiState {
    data object Loading : HistoryListUiState

    data class Success(
        val events: List<BatteryEvent>,
    ) : HistoryListUiState
}

@Inject
class HistoryListViewModel(
    private val deviceRepository: DeviceRepository,
) : ViewModel() {
    val uiState: StateFlow<HistoryListUiState> = deviceRepository
        .getAllEvents()
        .map { list -> HistoryListUiState.Success(list) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HistoryListUiState.Success(emptyList()),
        )
}
