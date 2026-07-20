package com.chriscartland.batterybutler.testcommon

import com.chriscartland.batterybutler.datalocal.DeviceImageCache
import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Fake implementation of [DeviceImageCache] for testing -- an in-memory map, nothing more. */
class FakeDeviceImageCache : DeviceImageCache {
    val entries = mutableMapOf<String, DeviceImageBytes>()
    private val observers = mutableMapOf<String, MutableStateFlow<DeviceImageBytes?>>()

    override suspend fun get(imageEtag: String): DeviceImageBytes? = entries[imageEtag]

    override fun observe(imageEtag: String): StateFlow<DeviceImageBytes?> = observers.getOrPut(imageEtag) { MutableStateFlow(entries[imageEtag]) }

    override suspend fun put(
        imageEtag: String,
        bytes: DeviceImageBytes,
    ) {
        entries[imageEtag] = bytes
        observers[imageEtag]?.value = bytes
    }

    override suspend fun evictExcept(keepEtags: Set<String>) {
        entries.keys.retainAll(keepEtags)
        observers.forEach { (etag, flow) -> if (etag !in keepEtags) flow.value = null }
    }
}
