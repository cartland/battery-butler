package com.chriscartland.batterybutler.datanetwork

import com.chriscartland.batterybutler.datanetwork.rest.RestDeviceImageDataSource
import com.chriscartland.batterybutler.datanetwork.rest.createSyncHttpClient
import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import com.chriscartland.batterybutler.domain.model.DeviceImageError
import com.chriscartland.batterybutler.domain.model.DispatcherProvider
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.repository.DataModeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

/**
 * Default [DeviceImageDataSource] -- dispatches to [RestDeviceImageDataSource] only in a Labs
 * data mode (the only backend that serves images), the same per-mode routing
 * [DelegatingRemoteDataSource] does for sync. Every other mode reports [supported] = false and
 * degrades gracefully (upload -> [DeviceImageError.NetworkError], fetch -> null, delete -> false)
 * rather than crashing -- callers are expected to gate the UI on [supported] first, but nothing
 * breaks if they don't. See `docs/DEVICE_IMAGES.md` §6B.
 */
@Inject
class DelegatingDeviceImageDataSource(
    private val dataModeRepository: DataModeRepository,
    private val labsAuthGateway: LabsAuthGateway,
    private val dispatcherProvider: DispatcherProvider,
) : DeviceImageDataSource {
    private val syncHttpClient by lazy { createSyncHttpClient() }

    override val supported: Flow<Boolean> = dataModeRepository.dataMode.map { it.isLabsMode() }

    override suspend fun upload(
        deviceId: String,
        bytes: ByteArray,
        contentType: String,
    ): Result<String, DeviceImageError> =
        currentRestDataSource()?.upload(deviceId, bytes, contentType)
            ?: Result.Error(DeviceImageError.NetworkError(message = "Device images are not supported by the current backend"))

    override suspend fun fetch(deviceId: String): DeviceImageBytes? = currentRestDataSource()?.fetch(deviceId)

    override suspend fun delete(deviceId: String): Boolean = currentRestDataSource()?.delete(deviceId) ?: false

    private suspend fun currentRestDataSource(): RestDeviceImageDataSource? {
        val url = when (val mode = dataModeRepository.dataMode.first()) {
            is DataMode.LabsStaging -> mode.url
            is DataMode.LabsProd -> mode.url
            else -> null
        } ?: return null
        return RestDeviceImageDataSource(
            httpClient = syncHttpClient,
            baseUrl = url,
            tokenProvider = labsAuthGateway::getLabsIdToken,
            dispatcherProvider = dispatcherProvider,
        )
    }

    private fun DataMode.isLabsMode(): Boolean = this is DataMode.LabsStaging || this is DataMode.LabsProd
}
