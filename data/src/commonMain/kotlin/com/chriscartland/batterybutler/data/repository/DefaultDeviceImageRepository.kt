package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.datalocal.DeviceImageCache
import com.chriscartland.batterybutler.datanetwork.DeviceImageDataSource
import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import com.chriscartland.batterybutler.domain.model.DeviceImageError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.repository.DeviceImageRepository
import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject

@Inject
class DefaultDeviceImageRepository(
    private val deviceImageDataSource: DeviceImageDataSource,
    private val deviceImageCache: DeviceImageCache,
) : DeviceImageRepository {
    override val supported: Flow<Boolean> = deviceImageDataSource.supported

    override fun observeCachedImage(imageEtag: String): Flow<DeviceImageBytes?> = deviceImageCache.observe(imageEtag)

    override suspend fun uploadImage(
        deviceId: String,
        bytes: ByteArray,
        contentType: String,
    ): Result<String, DeviceImageError> {
        val result = deviceImageDataSource.upload(deviceId, bytes, contentType)
        if (result is Result.Success) {
            deviceImageCache.put(result.data, DeviceImageBytes(bytes, contentType))
        }
        return result
    }

    override suspend fun deleteImage(deviceId: String): Boolean = deviceImageDataSource.delete(deviceId)
}
