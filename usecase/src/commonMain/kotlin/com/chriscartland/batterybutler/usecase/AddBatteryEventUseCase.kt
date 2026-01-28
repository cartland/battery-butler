package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import me.tatarka.inject.annotations.Inject

/**
 * Adds a battery replacement event and updates the device's last replaced timestamp.
 *
 * This use case handles the complete workflow of recording a battery replacement:
 * 1. Persists the battery event to the repository
 * 2. Updates the device's [batteryLastReplaced] timestamp if this event is the most recent
 */
@Inject
class AddBatteryEventUseCase(
    private val deviceRepository: DeviceRepository,
    private val updateDeviceLastReplaced: UpdateDeviceLastReplacedUseCase,
) {
    suspend operator fun invoke(event: BatteryEvent) {
        deviceRepository.addEvent(event)
        updateDeviceLastReplaced(event.deviceId)
    }
}
