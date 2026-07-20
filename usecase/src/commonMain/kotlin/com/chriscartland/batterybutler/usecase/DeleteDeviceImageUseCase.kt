package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.repository.DeviceImageRepository
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import me.tatarka.inject.annotations.Inject

/**
 * Deletes a device photo and, on success, clears the device's etag -- both steps run on [scope]
 * (an app-scoped [CoroutineScope]), for the same reason as [UploadDeviceImageUseCase]: it must
 * finish even if the screen that started it has already closed.
 */
@Inject
class DeleteDeviceImageUseCase(
    private val deviceImageRepository: DeviceImageRepository,
    private val deviceRepository: DeviceRepository,
    private val scope: CoroutineScope,
) {
    suspend operator fun invoke(deviceId: String): Boolean =
        scope.async {
            val success = deviceImageRepository.deleteImage(deviceId)
            if (success) {
                applyImageEtag(deviceId)
            }
            success
        }.await()

    private suspend fun applyImageEtag(deviceId: String) {
        val device = deviceRepository.getDeviceById(deviceId).first() ?: return
        deviceRepository.updateDevice(device.copy(imageEtag = null, lastUpdated = Clock.System.now()))
    }
}
