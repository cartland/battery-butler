package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.data.di.DynamicDatabaseProvider
import com.chriscartland.batterybutler.data.room.toDomain
import com.chriscartland.batterybutler.data.room.toEntity
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.domain.repository.RemoteDataSource
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
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
    private val dao get() = databaseProvider.database.value.deviceDao()

    init {
        scope.launch {
            try {
                remoteDataSource.subscribe().collect { update ->
                    if (update.isFullSnapshot) {
                        // TODO: Clear local DB? For now, we just insert/update
                    }
                    update.deviceTypes.forEach { dao.insertDeviceType(it.toEntity()) } // Upsert
                    update.devices.forEach { dao.insertDevice(it.toEntity()) }
                    update.events.forEach { dao.insertEvent(it.toEntity()) }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // Log error
                e.printStackTrace()
            }
        }
    }

    override fun getAllDevices(): Flow<List<Device>> =
        dao.getAllDevices().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getDeviceById(id: String): Flow<Device?> = dao.getDeviceById(id).map { it?.toDomain() }

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

    override fun getAllDeviceTypes(): Flow<List<DeviceType>> =
        dao.getAllDeviceTypes().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getDeviceTypeById(id: String): Flow<DeviceType?> = dao.getDeviceTypeById(id).map { it?.toDomain() }

    override suspend fun addDeviceType(type: DeviceType) {
        dao.insertDeviceType(type.toEntity())
    }

    override suspend fun updateDeviceType(type: DeviceType) {
        dao.updateDeviceType(type.toEntity())
    }

    override suspend fun deleteDeviceType(id: String) {
        dao.deleteDeviceType(id)
    }

    override fun getEventsForDevice(deviceId: String): Flow<List<BatteryEvent>> =
        dao.getEventsForDevice(deviceId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getAllEvents(): Flow<List<BatteryEvent>> =
        dao.getAllEvents().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getEventById(id: String): Flow<BatteryEvent?> = dao.getEventById(id).map { it?.toDomain() }

    override suspend fun addEvent(event: BatteryEvent) {
        dao.insertEvent(event.toEntity())
    }

    override suspend fun updateEvent(event: BatteryEvent) {
        dao.updateEvent(event.toEntity())
    }

    override suspend fun deleteEvent(id: String) {
        dao.deleteEvent(id)
    }

    // implementation removed
}
