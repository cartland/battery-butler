package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.datalocal.LocalDataSource
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.SyncStatus
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Inject

@Inject
class DefaultDeviceRepository(
    private val localDataSource: LocalDataSource,
    private val syncManager: SyncManager,
) : DeviceRepository {
    override val syncStatus: StateFlow<SyncStatus> = syncManager.syncStatus

    override fun dismissSyncStatus() = syncManager.dismissSyncStatus()

    override fun getAllDevices(): Flow<List<Device>> = localDataSource.getAllDevices()

    override fun getDeviceById(id: String): Flow<Device?> = localDataSource.getDeviceById(id)

    override suspend fun addDevice(device: Device) {
        localDataSource.addDevice(device)
        syncManager.pushUpdate(devices = listOf(device))
    }

    override suspend fun updateDevice(device: Device) {
        localDataSource.updateDevice(device)
        syncManager.pushUpdate(devices = listOf(device))
    }

    override suspend fun deleteDevice(id: String) {
        localDataSource.deleteDevice(id)
        syncManager.pushUpdate(deletedDeviceIds = listOf(id))
    }

    override fun getAllDeviceTypes(): Flow<List<DeviceType>> = localDataSource.getAllDeviceTypes()

    override fun getDeviceTypeById(id: String): Flow<DeviceType?> = localDataSource.getDeviceTypeById(id)

    override suspend fun addDeviceType(type: DeviceType) {
        localDataSource.addDeviceType(type)
        syncManager.pushUpdate(deviceTypes = listOf(type))
    }

    override suspend fun updateDeviceType(type: DeviceType) {
        localDataSource.updateDeviceType(type)
        syncManager.pushUpdate(deviceTypes = listOf(type))
    }

    override suspend fun deleteDeviceType(id: String) {
        localDataSource.deleteDeviceType(id)
        syncManager.pushUpdate(deletedDeviceTypeIds = listOf(id))
    }

    override fun getEventsForDevice(deviceId: String): Flow<List<BatteryEvent>> = localDataSource.getEventsForDevice(deviceId)

    override fun getAllEvents(): Flow<List<BatteryEvent>> = localDataSource.getAllEvents()

    override fun getEventById(id: String): Flow<BatteryEvent?> = localDataSource.getEventById(id)

    override suspend fun addEvent(event: BatteryEvent) {
        localDataSource.addEvent(event)
        syncManager.pushUpdate(events = listOf(event))
    }

    override suspend fun updateEvent(event: BatteryEvent) {
        localDataSource.updateEvent(event)
        syncManager.pushUpdate(events = listOf(event))
    }

    override suspend fun deleteEvent(id: String) {
        localDataSource.deleteEvent(id)
        syncManager.pushUpdate(deletedEventIds = listOf(id))
    }
}
