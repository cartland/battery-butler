package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.repository.DeviceImageRepository
import me.tatarka.inject.annotations.Inject

@Inject
class DeleteDeviceImageUseCase(
    private val deviceImageRepository: DeviceImageRepository,
) {
    suspend operator fun invoke(deviceId: String): Boolean = deviceImageRepository.deleteImage(deviceId)
}
