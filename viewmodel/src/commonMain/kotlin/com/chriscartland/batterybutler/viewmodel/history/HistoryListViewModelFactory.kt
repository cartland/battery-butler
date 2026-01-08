package com.chriscartland.batterybutler.viewmodel.history

import com.chriscartland.batterybutler.usecase.GetBatteryEventsUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.GetDevicesUseCase
import me.tatarka.inject.annotations.Inject

@Inject
class HistoryListViewModelFactory(
    private val getBatteryEventsUseCase: GetBatteryEventsUseCase,
    private val getDevicesUseCase: GetDevicesUseCase,
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
) {
    fun create(): HistoryListViewModel =
        HistoryListViewModel(
            getBatteryEventsUseCase,
            getDevicesUseCase,
            getDeviceTypesUseCase,
        )
}
