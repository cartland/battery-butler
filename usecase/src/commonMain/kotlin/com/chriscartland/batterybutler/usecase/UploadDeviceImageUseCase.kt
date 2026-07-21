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
                    val applyResult = applyImageEtag(deviceId, result.data)
                    if (applyResult is Result.Error) return@async applyResult
                }
                result
            }.await()

    /**
     * Writes [imageEtag] onto the device record. Both failure modes here were previously silent
     * (an early `return` on a missing device, and a discarded [Result] from [DeviceRepository.updateDevice])
     * -- the upload would report [Result.Success] to the caller even though the etag never actually
     * landed locally, so the photo would never appear even though the byte upload genuinely
     * succeeded. See `bb-dimg-image-not-shown` in TODO.md.
     */
    private suspend fun applyImageEtag(
        deviceId: String,
        imageEtag: String?,
    ): Result<Unit, DeviceImageError> {
        val device = deviceRepository.getDeviceById(deviceId).first()
            ?: return Result.Error(DeviceImageError.DeviceNotFound())
        return when (
            val result = deviceRepository.updateDevice(device.copy(imageEtag = imageEtag, lastUpdated = Clock.System.now()))
        ) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> Result.Error(DeviceImageError.NetworkError(cause = result.error.message))
        }
    }
}
