package com.chriscartland.batterybutler.datalocal

import com.chriscartland.batterybutler.datalocal.room.AppDatabase
import com.chriscartland.batterybutler.datalocal.room.DynamicDatabaseProvider
import com.chriscartland.batterybutler.datalocal.room.entity.NeedsBatteryFlagEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import me.tatarka.inject.annotations.Inject

@Inject
class RoomNeedsBatteryStore(
    private val databaseProvider: DynamicDatabaseProvider,
) : NeedsBatteryStore {
    // Read through the provider rather than binding a static AppDatabase, for the same reason
    // RoomDeviceImageCache does: a mode switch close()s the previous instance, and a Room Flow on
    // a closed database completes silently -- no emission, no exception -- which would freeze the
    // flag set at whatever it last read.
    private val dao get() = databaseProvider.database.value.needsBatteryFlagDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> bound(query: (AppDatabase) -> Flow<T>): Flow<T> =
        combine(
            databaseProvider.database,
            databaseProvider.rebindSignal.onStart { emit(0L) },
        ) { db, _ -> db }.flatMapLatest(query)

    override fun observeFlaggedDeviceIds(): Flow<Set<String>> =
        bound { db ->
            db.needsBatteryFlagDao().observeAll().map { flags -> flags.mapTo(mutableSetOf()) { it.deviceId } }
        }

    override suspend fun flag(
        deviceId: String,
        flaggedAt: Long,
    ) {
        dao.set(NeedsBatteryFlagEntity(deviceId = deviceId, flaggedAt = flaggedAt))
    }

    override suspend fun clear(deviceId: String) {
        dao.clear(deviceId)
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }
}
