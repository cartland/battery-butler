package com.chriscartland.batterybutler.datalocal

import com.chriscartland.batterybutler.datalocal.room.AppDatabase
import com.chriscartland.batterybutler.datalocal.room.DynamicDatabaseProvider
import com.chriscartland.batterybutler.datalocal.room.entity.toDomain
import com.chriscartland.batterybutler.datalocal.room.entity.toEntity
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import me.tatarka.inject.annotations.Inject

@Inject
class RoomLocalDataSource(
    private val databaseProvider: DynamicDatabaseProvider,
) : LocalDataSource {
    // Helper to get current DAO for suspend functions
    private val dao get() = databaseProvider.database.value.deviceDao()

    /**
     * Builds a Flow that re-subscribes to [query] whenever the active database
     * swaps OR the rebind signal ticks (currently emitted by
     * [DynamicDatabaseProvider.restoreFromLegacy]). Without observing the rebind
     * signal, Room `@Query` Flows can stay stuck on their initial value after a
     * file-level database restore — see bd issue bb-lg42.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> bound(query: (AppDatabase) -> Flow<T>): Flow<T> =
        combine(
            databaseProvider.database,
            databaseProvider.rebindSignal.onStart { emit(0L) },
        ) { db, _ -> db }.flatMapLatest(query)

    override fun getAllDevices(): Flow<List<Device>> =
        bound { db ->
            db.deviceDao().getAllDevices().map { entities ->
                entities.map { it.toDomain() }
            }
        }

    override fun getDeviceById(id: String): Flow<Device?> =
        bound { db ->
            db.deviceDao().getDeviceById(id).map { it?.toDomain() }
        }

    override suspend fun addDevice(device: Device) {
        dao.insertDevice(device.toEntity())
    }

    override suspend fun addDevices(devices: List<Device>) {
        dao.insertDevices(devices.map { it.toEntity() })
    }

    override suspend fun updateDevice(device: Device) {
        dao.updateDevice(device.toEntity())
    }

    override suspend fun deleteDevice(id: String) {
        dao.deleteDevice(id)
    }

    override fun getAllDeviceTypes(): Flow<List<DeviceType>> =
        bound { db ->
            db.deviceDao().getAllDeviceTypes().map { entities ->
                entities.map { it.toDomain() }
            }
        }

    override fun getDeviceTypeById(id: String): Flow<DeviceType?> =
        bound { db ->
            db.deviceDao().getDeviceTypeById(id).map { it?.toDomain() }
        }

    override suspend fun addDeviceType(type: DeviceType) {
        dao.insertDeviceType(type.toEntity())
    }

    override suspend fun addDeviceTypes(types: List<DeviceType>) {
        dao.insertDeviceTypes(types.map { it.toEntity() })
    }

    override suspend fun updateDeviceType(type: DeviceType) {
        dao.updateDeviceType(type.toEntity())
    }

    override suspend fun deleteDeviceType(id: String) {
        dao.deleteDeviceType(id)
    }

    override fun getEventsForDevice(deviceId: String): Flow<List<BatteryEvent>> =
        bound { db ->
            db.deviceDao().getEventsForDevice(deviceId).map { entities ->
                entities.map { it.toDomain() }
            }
        }

    override fun getAllEvents(): Flow<List<BatteryEvent>> =
        bound { db ->
            db.deviceDao().getAllEvents().map { entities ->
                entities.map { it.toDomain() }
            }
        }

    override fun getEventById(id: String): Flow<BatteryEvent?> =
        bound { db ->
            db.deviceDao().getEventById(id).map { it?.toDomain() }
        }

    override suspend fun addEvent(event: BatteryEvent) {
        dao.insertEvent(event.toEntity())
    }

    override suspend fun addEvents(events: List<BatteryEvent>) {
        dao.insertEvents(events.map { it.toEntity() })
    }

    override suspend fun updateEvent(event: BatteryEvent) {
        dao.updateEvent(event.toEntity())
    }

    override suspend fun deleteEvent(id: String) {
        dao.deleteEvent(id)
    }

    override suspend fun clearAll() {
        dao.deleteAllDevices()
        dao.deleteAllDeviceTypes()
        dao.deleteAllEvents()
    }
}
