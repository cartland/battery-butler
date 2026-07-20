package com.chriscartland.batterybutler.domain.repository

import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import com.chriscartland.batterybutler.domain.model.DeviceImageError
import com.chriscartland.batterybutler.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * App-facing seam for device photos -- the `usecase`/`viewmodel` layers' entry point to both the
 * on-device cache (reads, populated by sync) and the Labs backend's image endpoints (writes).
 * See `docs/DEVICE_IMAGES.md`.
 */
interface DeviceImageRepository {
    /** True only when the currently-selected backend supports device images (Labs modes). */
    val supported: Flow<Boolean>

    /**
     * The cached bytes for [imageEtag], reactive -- emits again once sync populates it. Never
     * fetches on demand: the cache is populated as a side effect of sync (see
     * `DeviceImageSyncCoordinator`); null just means "not cached yet," not an error, and the UI
     * should keep showing its fallback icon until a non-null value arrives.
     */
    fun observeCachedImage(imageEtag: String): Flow<DeviceImageBytes?>

    /**
     * Upload (or replace) [deviceId]'s photo and cache the bytes under the returned etag so the UI
     * can show it immediately, without waiting for the next sync. The device must already be
     * synced -- an unknown device yields [DeviceImageError.DeviceNotFound].
     */
    suspend fun uploadImage(
        deviceId: String,
        bytes: ByteArray,
        contentType: String,
    ): Result<String, DeviceImageError>

    /** Remove [deviceId]'s photo. Idempotent -- true even if it had none. False only on failure. */
    suspend fun deleteImage(deviceId: String): Boolean
}
