package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.first
import me.tatarka.inject.annotations.Inject

@Inject
class AddBatteryEventUseCase(
    private val deviceRepository: DeviceRepository,
) {
    suspend operator fun invoke(event: BatteryEvent) {
        deviceRepository.addEvent(event)
        val device = deviceRepository.getDeviceById(event.deviceId).first() ?: return

        // We just added an event, but we should check all events to find the latest
        val events = deviceRepository.getEventsForDevice(event.deviceId).first()
        val latestEvent = events.maxByOrNull { it.date }

        if (latestEvent != null && latestEvent.date > device.batteryLastReplaced) {
            deviceRepository.updateDevice(device.copy(batteryLastReplaced = latestEvent.date))
        }
    }
}
