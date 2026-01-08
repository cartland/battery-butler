package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject

@Inject
class GetEventDetailUseCase(
    private val deviceRepository: DeviceRepository,
) {
    operator fun invoke(eventId: String): Flow<BatteryEvent?> = deviceRepository.getEventById(eventId)
}
