package com.chriscartland.batterybutler.datalocal.repository

import com.chriscartland.batterybutler.datalocal.di.DynamicDatabaseProvider
import com.chriscartland.batterybutler.datalocal.room.toDomain
import com.chriscartland.batterybutler.datalocal.room.toEntity
import com.chriscartland.batterybutler.domain.AppLogger
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.domain.repository.RemoteDataSource
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import kotlin.coroutines.cancellation.CancellationException

@Inject
class RoomDeviceRepository(
    private val databaseProvider: DynamicDatabaseProvider,
    private val remoteDataSource: RemoteDataSource,
    private val scope: CoroutineScope,
) : DeviceRepository {
    // Helper to get current DAO for suspend functions
    private val dao get() = databaseProvider.database.value.deviceDao()

    init {
        scope.launch {
            try {
                remoteDataSource.subscribe().collect { update ->
                    AppLogger.d("BatteryButlerRepo", "RoomDeviceRepository received update! Size=${update.devices.size}")
                    val currentDao = databaseProvider.database.value.deviceDao()
                    if (update.isFullSnapshot) {
                        // TODO: Clear local DB? For now, we just insert/update
                    }
                    update.deviceTypes.forEach { currentDao.insertDeviceType(it.toEntity()) } // Upsert
                    update.devices.forEach { currentDao.insertDevice(it.toEntity()) }
                    update.events.forEach { currentDao.insertEvent(it.toEntity()) }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // Log error quietly unless critical
                // AppLogger.d("BatteryButlerDebug", "RoomDeviceRepository subscribe error: $e")
                e.printStackTrace()
            }
        }
    }

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
        scope.launch {
            remoteDataSource.push(
                RemoteUpdate(
                    isFullSnapshot = false,
                    deviceTypes = emptyList(),
                    devices = listOf(device),
                    events = emptyList(),
                ),
            )
        }
    }

    override suspend fun updateDevice(device: Device) {
        dao.updateDevice(device.toEntity())
        scope.launch {
            remoteDataSource.push(
                RemoteUpdate(
                    isFullSnapshot = false,
                    deviceTypes = emptyList(),
                    devices = listOf(device),
                    events = emptyList(),
                ),
            )
        }
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

    override suspend fun updateEvent(event: BatteryEvent) {
        dao.updateEvent(event.toEntity())
    }

    override suspend fun deleteEvent(id: String) {
        dao.deleteEvent(id)
    }
}
