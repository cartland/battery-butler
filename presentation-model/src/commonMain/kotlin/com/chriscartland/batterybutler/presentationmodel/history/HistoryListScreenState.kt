package com.chriscartland.batterybutler.presentationmodel.history

import com.chriscartland.batterybutler.domain.model.BatteryEvent

sealed interface HistoryListScreenState {
    data object Loading : HistoryListScreenState

    data class Success(
        val items: List<HistoryItemModel>,
    ) : HistoryListScreenState

    data class Error(
        val message: String,
    ) : HistoryListScreenState
}

data class HistoryItemModel(
    val event: BatteryEvent,
    val deviceName: String,
    val deviceTypeName: String,
    val deviceLocation: String?,
)
