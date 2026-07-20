package com.chriscartland.batterybutler.datalocal.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chriscartland.batterybutler.datalocal.room.entity.DeviceImageCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceImageCacheDao {
    @Query("SELECT * FROM device_image_cache WHERE imageEtag = :imageEtag")
    suspend fun get(imageEtag: String): DeviceImageCacheEntity?

    /** Reactive read for the UI -- emits again once sync populates the row. */
    @Query("SELECT * FROM device_image_cache WHERE imageEtag = :imageEtag")
    fun observe(imageEtag: String): Flow<DeviceImageCacheEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: DeviceImageCacheEntity)

    /** Drops every cached image whose etag isn't in [keepEtags] -- see `docs/DEVICE_IMAGES.md` §6D. */
    @Query("DELETE FROM device_image_cache WHERE imageEtag NOT IN (:keepEtags)")
    suspend fun evictExcept(keepEtags: List<String>)
}
