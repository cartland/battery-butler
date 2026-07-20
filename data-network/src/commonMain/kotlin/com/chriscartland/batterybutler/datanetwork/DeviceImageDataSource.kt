package com.chriscartland.batterybutler.datanetwork

import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import com.chriscartland.batterybutler.domain.model.DeviceImageError
import com.chriscartland.batterybutler.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * A capability interface, separate from [RemoteDataSource], for the per-device photo endpoints
 * the Labs backend exposes (`PUT/GET/DELETE .../devices/{id}/image`). Deliberately not folded
 * into [RemoteDataSource] -- images are a Labs-only capability, and widening the sync interface
 * would force Mock/gRPC/None implementations to grow no-op image methods they can never
 * meaningfully serve. See `docs/DEVICE_IMAGES.md` §6B.
 *
 * [supported] is the UI-facing capability gate ("show photo affordances only when the current
 * backend supports images") -- true only in a Labs data mode. The DI-provided implementation
 * ([DelegatingDeviceImageDataSource]) dispatches by the current [com.chriscartland.batterybutler
 * .domain.model.DataMode] the same way [DelegatingRemoteDataSource] does for sync, so callers
 * never need to check the mode themselves.
 */
interface DeviceImageDataSource {
    /** True only when the currently-selected backend supports device images (Labs modes). */
    val supported: Flow<Boolean>

    /**
     * Upload (or replace) [deviceId]'s photo. The device must already be synced -- an unknown
     * device yields [DeviceImageError.DeviceNotFound]. Returns the new `imageEtag` on success.
     */
    suspend fun upload(
        deviceId: String,
        bytes: ByteArray,
        contentType: String,
    ): Result<String, DeviceImageError>

    /** Fetch [deviceId]'s photo bytes, or null if it has none, isn't accessible, or the call failed. */
    suspend fun fetch(deviceId: String): DeviceImageBytes?

    /** Remove [deviceId]'s photo. Idempotent -- true even if it had none. False only on failure. */
    suspend fun delete(deviceId: String): Boolean
}
