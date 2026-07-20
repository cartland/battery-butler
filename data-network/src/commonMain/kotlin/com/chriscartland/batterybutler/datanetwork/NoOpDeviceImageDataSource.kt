package com.chriscartland.batterybutler.datanetwork

import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import com.chriscartland.batterybutler.domain.model.DeviceImageError
import com.chriscartland.batterybutler.domain.model.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * No-op [DeviceImageDataSource] for platforms where Labs image support isn't wired (the native
 * SwiftUI iOS app -- same reasoning as [com.chriscartland.batterybutler.domain.repository
 * .NoOpLabsAuthRepository]: Labs sign-in isn't wired there either, and device images need a
 * signed-in Labs session). Always reports unsupported; callers are expected to gate on
 * [supported] first, but every method degrades gracefully even if they don't.
 */
data object NoOpDeviceImageDataSource : DeviceImageDataSource {
    override val supported: StateFlow<Boolean> = MutableStateFlow(false)

    override suspend fun upload(
        deviceId: String,
        bytes: ByteArray,
        contentType: String,
    ): Result<String, DeviceImageError> =
        Result.Error(
            DeviceImageError.NetworkError(message = "Device images are not supported on this platform"),
        )

    override suspend fun fetch(deviceId: String): DeviceImageBytes? = null

    override suspend fun delete(deviceId: String): Boolean = false
}
