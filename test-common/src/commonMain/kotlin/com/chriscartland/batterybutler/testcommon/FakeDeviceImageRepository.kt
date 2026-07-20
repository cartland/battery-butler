package com.chriscartland.batterybutler.testcommon

import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import com.chriscartland.batterybutler.domain.model.DeviceImageError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.repository.DeviceImageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Fake implementation of [DeviceImageRepository] for testing. Unsupported by default. */
class FakeDeviceImageRepository : DeviceImageRepository {
    private val supportedFlow = MutableStateFlow(false)
    override val supported: StateFlow<Boolean> = supportedFlow
    private val cache = mutableMapOf<String, MutableStateFlow<DeviceImageBytes?>>()

    var uploadResult: Result<String, DeviceImageError> = Result.Success("etag-1")
    var deleteResult = true
    val deletedDeviceIds = mutableListOf<String>()

    fun setSupported(value: Boolean) {
        supportedFlow.value = value
    }

    /** Directly seeds the cache, as if a prior sync had already fetched [imageEtag]'s bytes. */
    fun setCachedImage(
        imageEtag: String,
        bytes: DeviceImageBytes?,
    ) {
        cache.getOrPut(imageEtag) { MutableStateFlow(null) }.value = bytes
    }

    override fun observeCachedImage(imageEtag: String): Flow<DeviceImageBytes?> = cache.getOrPut(imageEtag) { MutableStateFlow(null) }

    override suspend fun uploadImage(
        deviceId: String,
        bytes: ByteArray,
        contentType: String,
    ): Result<String, DeviceImageError> {
        val result = uploadResult
        if (result is Result.Success) {
            setCachedImage(result.data, DeviceImageBytes(bytes, contentType))
        }
        return result
    }

    override suspend fun deleteImage(deviceId: String): Boolean {
        deletedDeviceIds.add(deviceId)
        return deleteResult
    }
}
