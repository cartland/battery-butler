package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import me.tatarka.inject.annotations.Inject

@Inject
class DeleteDeviceTypeUseCase(
    private val deviceRepository: DeviceRepository,
) {
    suspend operator fun invoke(typeId: String) = deviceRepository.deleteDeviceType(typeId)
}
