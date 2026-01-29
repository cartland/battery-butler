package com.chriscartland.batterybutler.datalocal

import com.chriscartland.batterybutler.datalocal.room.DynamicDatabaseProvider
import com.chriscartland.batterybutler.datalocal.room.entity.toDomain
import com.chriscartland.batterybutler.datalocal.room.entity.toEntity
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

@Inject
class RoomLocalDataSource(
    private val databaseProvider: DynamicDatabaseProvider,
) : LocalDataSource {
    // Helper to get current DAO for suspend functions
    private val dao get() = databaseProvider.database.value.deviceDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAllDevices(): Flow<List<Device>> =
        databaseProvider.database.flatMapLatest { db ->
            db.deviceDao().getAllDevices().map { entities ->
                entities.map { it.toDomain() }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getDeviceById(id: String): Flow<Device?> =
        databaseProvider.database.flatMapLatest { db ->
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAllDeviceTypes(): Flow<List<DeviceType>> =
        databaseProvider.database.flatMapLatest { db ->
            db.deviceDao().getAllDeviceTypes().map { entities ->
                entities.map { it.toDomain() }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getDeviceTypeById(id: String): Flow<DeviceType?> =
        databaseProvider.database.flatMapLatest { db ->
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getEventsForDevice(deviceId: String): Flow<List<BatteryEvent>> =
        databaseProvider.database.flatMapLatest { db ->
            db.deviceDao().getEventsForDevice(deviceId).map { entities ->
                entities.map { it.toDomain() }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAllEvents(): Flow<List<BatteryEvent>> =
        databaseProvider.database.flatMapLatest { db ->
            db.deviceDao().getAllEvents().map { entities ->
                entities.map { it.toDomain() }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getEventById(id: String): Flow<BatteryEvent?> =
        databaseProvider.database.flatMapLatest { db ->
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
}
