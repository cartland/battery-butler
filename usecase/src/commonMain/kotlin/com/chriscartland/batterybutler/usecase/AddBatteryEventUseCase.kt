package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.flatMap
import com.chriscartland.batterybutler.domain.model.map
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import me.tatarka.inject.annotations.Inject

/**
 * Adds a battery replacement event and updates the device's last replaced timestamp.
 *
 * This use case handles the complete workflow of recording a battery replacement:
 * 1. Persists the battery event to the repository
 * 2. Updates the device's [batteryLastReplaced] timestamp if this event is the most recent
 * 3. Clears any "needs a new battery" mark -- the job is now done
 */
@Inject
class AddBatteryEventUseCase(
    private val deviceRepository: DeviceRepository,
    private val updateDeviceLastReplaced: UpdateDeviceLastReplacedUseCase,
    private val setDeviceNeedsBattery: SetDeviceNeedsBatteryUseCase,
) {
    suspend operator fun invoke(event: BatteryEvent): Result<Unit, DataError> =
        deviceRepository
            .addEvent(event)
            .flatMap {
                updateDeviceLastReplaced(event.deviceId).map { }
            }.flatMap {
                // Clearing the mark is part of recording the replacement: leaving it set would show a
                // device as still needing a battery immediately after the user logged changing it.
                setDeviceNeedsBattery(event.deviceId, needsBattery = false)
            }
}
