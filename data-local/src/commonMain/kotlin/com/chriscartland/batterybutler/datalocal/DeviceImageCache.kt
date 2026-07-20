package com.chriscartland.batterybutler.datalocal

import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import kotlinx.coroutines.flow.Flow

/**
 * On-device cache of device photo bytes, keyed by the content-addressed `imageEtag` (see
 * [com.chriscartland.batterybutler.domain.model.Device.imageEtag]). See `docs/DEVICE_IMAGES.md`
 * §6D.
 */
interface DeviceImageCache {
    /** The cached bytes for [imageEtag], or null if not cached. One-shot -- used by sync to decide whether a fetch is needed. */
    suspend fun get(imageEtag: String): DeviceImageBytes?

    /** Reactive read for the UI -- emits again once sync populates [imageEtag] (initially null -> the fallback icon shows until then). */
    fun observe(imageEtag: String): Flow<DeviceImageBytes?>

    /** Caches [bytes] under [imageEtag], overwriting any previous entry (there shouldn't be one -- etags are content-addressed). */
    suspend fun put(
        imageEtag: String,
        bytes: DeviceImageBytes,
    )

    /** Drops every cached entry whose etag isn't in [keepEtags]. */
    suspend fun evictExcept(keepEtags: Set<String>)
}
