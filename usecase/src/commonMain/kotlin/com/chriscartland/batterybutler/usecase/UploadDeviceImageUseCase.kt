package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.DeviceImageError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.repository.DeviceImageRepository
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import me.tatarka.inject.annotations.Inject
import kotlin.time.Clock

/**
 * Uploads a device photo and, on success, writes the returned etag onto the device -- both steps
 * run on [scope] (an app-scoped [CoroutineScope], not the caller's own scope). A ViewModel's
 * `viewModelScope` is cancelled the moment its screen closes (Save/Back right after picking a
 * photo, before the upload finishes); running the upload and the device write on the app scope
 * instead means the byte transfer and its bookkeeping always complete, even if the caller stops
 * waiting.
 */
@Inject
class UploadDeviceImageUseCase(
    private val deviceImageRepository: DeviceImageRepository,
    private val deviceRepository: DeviceRepository,
    private val scope: CoroutineScope,
) {
    suspend operator fun invoke(
        deviceId: String,
        bytes: ByteArray,
        contentType: String,
    ): Result<String, DeviceImageError> =
        scope
            .async {
                val result = deviceImageRepository.uploadImage(deviceId, bytes, contentType)
                if (result is Result.Success) {
                    applyImageEtag(deviceId, result.data)
                }
                result
            }.await()

    private suspend fun applyImageEtag(
        deviceId: String,
        imageEtag: String?,
    ) {
        val device = deviceRepository.getDeviceById(deviceId).first() ?: return
        deviceRepository.updateDevice(device.copy(imageEtag = imageEtag, lastUpdated = Clock.System.now()))
    }
}
