package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.flatMap
import com.chriscartland.batterybutler.domain.model.map
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.first
import me.tatarka.inject.annotations.Inject

@Inject
class DeleteBatteryEventUseCase(
    private val deviceRepository: DeviceRepository,
    private val updateDeviceLastReplaced: UpdateDeviceLastReplacedUseCase,
) {
    suspend operator fun invoke(eventId: String): Result<Unit, DataError> {
        val event = deviceRepository.getEventById(eventId).first()
        return deviceRepository.deleteEvent(eventId).flatMap {
            if (event != null) {
                updateDeviceLastReplaced(event.deviceId).map { }
            } else {
                Result.Success(Unit)
            }
        }
    }
}
