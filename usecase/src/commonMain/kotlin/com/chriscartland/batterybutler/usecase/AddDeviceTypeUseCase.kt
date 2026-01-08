package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import me.tatarka.inject.annotations.Inject

@Inject
class AddDeviceTypeUseCase(
    private val deviceRepository: DeviceRepository,
) {
    suspend operator fun invoke(deviceType: DeviceType) = deviceRepository.addDeviceType(deviceType)
}
