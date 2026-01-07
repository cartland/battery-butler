package com.chriscartland.batterybutler.feature.history

import com.chriscartland.batterybutler.domain.repository.DeviceRepository

class HistoryListViewModelFactory(
    private val deviceRepository: DeviceRepository,
) {
    fun create(): HistoryListViewModel = HistoryListViewModel(deviceRepository)
}
