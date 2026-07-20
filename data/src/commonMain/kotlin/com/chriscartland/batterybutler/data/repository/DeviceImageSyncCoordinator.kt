package com.chriscartland.batterybutler.data.repository

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.datalocal.DeviceImageCache
import com.chriscartland.batterybutler.datanetwork.DeviceImageDataSource
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

/**
 * Fetches and caches device photos after a sync, so the UI has bytes to render without a
 * per-device network round trip. See `docs/DEVICE_IMAGES.md` §6D.
 *
 * Fire-and-forget by design (like the rest of sync): [onRemoteUpdate] launches on [scope] and
 * returns immediately. A fetch failure for one device just leaves that device showing its
 * fallback icon -- it doesn't affect sync, other devices, or get retried until the next update.
 */
@Inject
class DeviceImageSyncCoordinator(
    private val deviceImageDataSource: DeviceImageDataSource,
    private val deviceImageCache: DeviceImageCache,
    private val scope: CoroutineScope,
) {
    fun onRemoteUpdate(update: RemoteUpdate) {
        scope.launch {
            if (!deviceImageDataSource.supported.first()) return@launch

            // Eviction needs the *complete* current device set to know what's still referenced --
            // only a full snapshot (not a partial push-derived patch) has that. imageEtag only ever
            // flows in from a real snapshot anyway (see RestSyncMapper), so this never skips a
            // meaningful eviction in practice.
            if (update.isFullSnapshot) {
                deviceImageCache.evictExcept(update.devices.mapNotNull { it.imageEtag }.toSet())
            }

            update.devices.forEach { device ->
                val etag = device.imageEtag ?: return@forEach
                if (deviceImageCache.get(etag) != null) return@forEach
                val bytes = deviceImageDataSource.fetch(device.id)
                if (bytes == null) {
                    Logger.w(TAG) { "Failed to fetch image for device ${device.id} (etag=$etag)" }
                    return@forEach
                }
                deviceImageCache.put(etag, bytes)
            }
        }
    }

    private companion object {
        const val TAG = "DeviceImageSyncCoordinator"
    }
}
