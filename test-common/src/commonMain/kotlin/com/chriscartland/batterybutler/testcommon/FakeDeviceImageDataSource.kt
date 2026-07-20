package com.chriscartland.batterybutler.testcommon

import com.chriscartland.batterybutler.datanetwork.DeviceImageDataSource
import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import com.chriscartland.batterybutler.domain.model.DeviceImageError
import com.chriscartland.batterybutler.domain.model.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fake implementation of [DeviceImageDataSource] for testing. Defaults to unsupported (like
 * Mock/gRPC/None data modes) -- set [supportedFlow] to true to exercise the upload/fetch/delete
 * paths, which serve from [uploadedBytes] and record calls in [fetchedDeviceIds]/[deletedDeviceIds].
 */
class FakeDeviceImageDataSource : DeviceImageDataSource {
    private val supportedFlow = MutableStateFlow(false)
    override val supported: StateFlow<Boolean> = supportedFlow

    var uploadResult: Result<String, DeviceImageError> = Result.Success("etag-1")
    val uploadedBytes = mutableMapOf<String, DeviceImageBytes>()
    val fetchedDeviceIds = mutableListOf<String>()
    val deletedDeviceIds = mutableListOf<String>()
    var deleteResult = true

    fun setSupported(value: Boolean) {
        supportedFlow.value = value
    }

    override suspend fun upload(
        deviceId: String,
        bytes: ByteArray,
        contentType: String,
    ): Result<String, DeviceImageError> {
        if (uploadResult is Result.Success) {
            uploadedBytes[deviceId] = DeviceImageBytes(bytes, contentType)
        }
        return uploadResult
    }

    override suspend fun fetch(deviceId: String): DeviceImageBytes? {
        fetchedDeviceIds.add(deviceId)
        return uploadedBytes[deviceId]
    }

    override suspend fun delete(deviceId: String): Boolean {
        deletedDeviceIds.add(deviceId)
        uploadedBytes.remove(deviceId)
        return deleteResult
    }
}
