package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import me.tatarka.inject.annotations.Inject

@Inject
class UpdateDeviceUseCase(
    private val deviceRepository: DeviceRepository,
) {
    suspend operator fun invoke(device: Device) = deviceRepository.updateDevice(device)
}
