package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject

@Inject
class GetBatteryEventsUseCase(
    private val deviceRepository: DeviceRepository,
) {
    operator fun invoke(): Flow<List<BatteryEvent>> = deviceRepository.getAllEvents()

    fun forDevice(deviceId: String): Flow<List<BatteryEvent>> = deviceRepository.getEventsForDevice(deviceId)
}
