package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import me.tatarka.inject.annotations.Inject

@Inject
class DeleteBatteryEventUseCase(
    private val deviceRepository: DeviceRepository,
) {
    suspend operator fun invoke(eventId: String) = deviceRepository.deleteEvent(eventId)
}
