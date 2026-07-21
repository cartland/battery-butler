package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.repository.DeviceImageRepository
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import me.tatarka.inject.annotations.Inject
import kotlin.time.Clock

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
        scope
            .async {
                val success = deviceImageRepository.deleteImage(deviceId)
                success && applyImageEtag(deviceId)
            }.await()

    /**
     * Clears the device's etag; returns false (not silently true) if the device can't be found or
     * the local write fails, so a failure here doesn't get reported as a successful removal. See
     * the matching fix in [UploadDeviceImageUseCase.applyImageEtag] / `bb-dimg-image-not-shown` in
     * TODO.md.
     */
    private suspend fun applyImageEtag(deviceId: String): Boolean {
        val device = deviceRepository.getDeviceById(deviceId).first() ?: return false
        return deviceRepository.updateDevice(device.copy(imageEtag = null, lastUpdated = Clock.System.now())) is Result.Success
    }
}
