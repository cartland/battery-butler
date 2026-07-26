package com.chriscartland.batterybutler.datalocal

import com.chriscartland.batterybutler.datalocal.room.AppDatabase
import com.chriscartland.batterybutler.datalocal.room.DynamicDatabaseProvider
import com.chriscartland.batterybutler.datalocal.room.entity.DeviceImageCacheEntity
import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import me.tatarka.inject.annotations.Inject

@Inject
class RoomDeviceImageCache(
    private val databaseProvider: DynamicDatabaseProvider,
) : DeviceImageCache {
    // Follow the active database via DynamicDatabaseProvider, exactly like RoomLocalDataSource.
    // Binding to a static AppDatabase (DatabaseOption.OFFLINE) meant that after a mode switch --
    // e.g. signing into Labs -- the provider close()d + evicted that OFFLINE instance while the
    // cache kept querying it. A Room `@Query` Flow on a closed database completes silently (no
    // emission, no exception), which wedged Device Details / Edit Device at Loading forever for
    // any device with an image. Reading through the provider keeps the cache on the same live
    // database as the device rows.
    private val dao get() = databaseProvider.database.value.deviceImageCacheDao()

    /**
     * Builds a Flow that re-subscribes to [query] whenever the active database swaps OR the
     * rebind signal ticks -- the same liveness mechanism [RoomLocalDataSource.bound] uses so a
     * mode switch or file-level restore never leaves the Flow stuck on a stale/closed database.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> bound(query: (AppDatabase) -> Flow<T>): Flow<T> =
        combine(
            databaseProvider.database,
            databaseProvider.rebindSignal.onStart { emit(0L) },
        ) { db, _ -> db }.flatMapLatest(query)

    override suspend fun get(imageEtag: String): DeviceImageBytes? = dao.get(imageEtag)?.let { DeviceImageBytes(bytes = it.bytes, contentType = it.contentType) }

    override fun observe(imageEtag: String): Flow<DeviceImageBytes?> =
        bound { db ->
            db.deviceImageCacheDao().observe(imageEtag).map { entity -> entity?.let { DeviceImageBytes(bytes = it.bytes, contentType = it.contentType) } }
        }

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
