package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import me.tatarka.inject.annotations.Inject

@Inject
class UpdateBatteryEventUseCase(
    private val deviceRepository: DeviceRepository,
) {
    suspend operator fun invoke(event: BatteryEvent) = deviceRepository.updateEvent(event)
}
