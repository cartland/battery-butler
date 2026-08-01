package com.chriscartland.batterybutler.presentationmodel.history

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.presentationmodel.home.DensityOption

sealed interface HistoryListScreenState {
    data object Loading : HistoryListScreenState

    data class Success(
        val items: List<HistoryItemModel>,
        val densityOption: DensityOption = DensityOption.EXPANDED,
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
