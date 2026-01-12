package com.chriscartland.batterybutler.presentation.models.history

import com.chriscartland.batterybutler.domain.model.BatteryEvent

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
