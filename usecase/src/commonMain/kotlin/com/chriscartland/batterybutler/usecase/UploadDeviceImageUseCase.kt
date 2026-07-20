package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.DeviceImageError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.repository.DeviceImageRepository
import me.tatarka.inject.annotations.Inject

@Inject
class UploadDeviceImageUseCase(
    private val deviceImageRepository: DeviceImageRepository,
) {
    suspend operator fun invoke(
        deviceId: String,
        bytes: ByteArray,
        contentType: String,
    ): Result<String, DeviceImageError> = deviceImageRepository.uploadImage(deviceId, bytes, contentType)
}
