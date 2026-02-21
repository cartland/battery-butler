package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.first
import me.tatarka.inject.annotations.Inject

@Inject
class DeleteBatteryEventUseCase(
    private val deviceRepository: DeviceRepository,
    private val updateDeviceLastReplaced: UpdateDeviceLastReplacedUseCase,
) {
    suspend operator fun invoke(eventId: String) {
        val event = deviceRepository.getEventById(eventId).first()
        deviceRepository.deleteEvent(eventId)
        if (event != null) {
            updateDeviceLastReplaced(event.deviceId)
        }
    }
}
