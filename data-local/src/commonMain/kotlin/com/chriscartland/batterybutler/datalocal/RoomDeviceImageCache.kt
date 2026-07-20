package com.chriscartland.batterybutler.datalocal

import com.chriscartland.batterybutler.datalocal.room.AppDatabase
import com.chriscartland.batterybutler.datalocal.room.entity.DeviceImageCacheEntity
import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

@Inject
class RoomDeviceImageCache(
    private val database: AppDatabase,
) : DeviceImageCache {
    private val dao get() = database.deviceImageCacheDao()

    override suspend fun get(imageEtag: String): DeviceImageBytes? = dao.get(imageEtag)?.let { DeviceImageBytes(bytes = it.bytes, contentType = it.contentType) }

    override fun observe(imageEtag: String): Flow<DeviceImageBytes?> = dao.observe(imageEtag).map { entity -> entity?.let { DeviceImageBytes(bytes = it.bytes, contentType = it.contentType) } }

    override suspend fun put(
        imageEtag: String,
        bytes: DeviceImageBytes,
    ) {
        dao.put(DeviceImageCacheEntity(imageEtag = imageEtag, bytes = bytes.bytes, contentType = bytes.contentType))
    }

    override suspend fun evictExcept(keepEtags: Set<String>) {
        dao.evictExcept(keepEtags.toList())
    }
}
