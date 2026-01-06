package com.chriscartland.blanket.feature.history

import com.chriscartland.blanket.domain.repository.DeviceRepository

class HistoryListViewModelFactory(
    private val deviceRepository: DeviceRepository
) {
    fun create(): HistoryListViewModel {
        return HistoryListViewModel(deviceRepository)
    }
}
