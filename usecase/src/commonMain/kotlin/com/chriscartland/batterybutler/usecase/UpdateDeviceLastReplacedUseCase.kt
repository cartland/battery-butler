package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.map
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.first
import me.tatarka.inject.annotations.Inject
import kotlin.time.Instant

/**
 * Updates a device's [batteryLastReplaced] timestamp to the most recent battery event date.
 *
 * This use case queries all battery events for the device and sets the device's
 * [batteryLastReplaced] to the date of the most recent event. If no events remain,
 * it resets the timestamp to epoch 0.
 *
 * This encapsulates the business rule: "A device's last replaced date should always
 * reflect the most recent battery replacement event."
 */
@Inject
class UpdateDeviceLastReplacedUseCase(
    private val deviceRepository: DeviceRepository,
) {
    /**
     * Updates the device's batteryLastReplaced timestamp based on its events.
     *
     * @param deviceId The ID of the device to update
     * @return [Result.Success] with true if the device was updated, false if no update was needed
     */
    suspend operator fun invoke(deviceId: String): Result<Boolean, DataError> {
        val device = deviceRepository.getDeviceById(deviceId).first() ?: return Result.Success(false)
        val events = deviceRepository.getEventsForDevice(deviceId).first()
        val latestDate = events.maxByOrNull { it.date }?.date ?: Instant.fromEpochMilliseconds(0)

        return if (latestDate != device.batteryLastReplaced) {
            deviceRepository.updateDevice(device.copy(batteryLastReplaced = latestDate)).map { true }
        } else {
            Result.Success(false)
        }
    }

    /**
     * Updates the device's batteryLastReplaced timestamp if the given timestamp is newer.
     *
     * This is a simpler version that doesn't query all events, useful when you already
     * know the timestamp of the event that was just added.
     *
     * @param deviceId The ID of the device to update
     * @param newTimestamp The timestamp to compare against
     * @return [Result.Success] with true if the device was updated, false if no update was needed
     */
    suspend fun ifNewer(
        deviceId: String,
        newTimestamp: Instant,
    ): Result<Boolean, DataError> {
        val device = deviceRepository.getDeviceById(deviceId).first() ?: return Result.Success(false)

        return if (newTimestamp > device.batteryLastReplaced) {
            deviceRepository.updateDevice(device.copy(batteryLastReplaced = newTimestamp)).map { true }
        } else {
            Result.Success(false)
        }
    }
}
