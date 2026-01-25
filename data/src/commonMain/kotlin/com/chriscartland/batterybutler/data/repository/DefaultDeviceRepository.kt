package com.chriscartland.batterybutler.data.repository

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.datalocal.LocalDataSource
import com.chriscartland.batterybutler.datanetwork.RemoteDataSource
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import kotlin.coroutines.cancellation.CancellationException

@Inject
class DefaultDeviceRepository(
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource,
    private val scope: CoroutineScope,
) : DeviceRepository {
    init {
        scope.launch {
            try {
                remoteDataSource.subscribe().collect { update ->
                    Logger.d("BatteryButlerRepo") { "DefaultDeviceRepository received update! Size=${update.devices.size}" }

                    if (update.isFullSnapshot) {
                        // TODO: Clear local DB? For now, we just insert/update
                    }
                    update.deviceTypes.forEach { localDataSource.addDeviceType(it) } // Assuming add handles upsert
                    update.devices.forEach { localDataSource.addDevice(it) }
                    update.events.forEach { localDataSource.addEvent(it) }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // Log error quietly unless critical
                e.printStackTrace()
            }
        }
    }

    override fun getAllDevices(): Flow<List<Device>> = localDataSource.getAllDevices()

    override fun getDeviceById(id: String): Flow<Device?> = localDataSource.getDeviceById(id)

    override suspend fun addDevice(device: Device) {
        localDataSource.addDevice(device)
        pushUpdate(devices = listOf(device))
    }

    override suspend fun updateDevice(device: Device) {
        localDataSource.updateDevice(device)
        pushUpdate(devices = listOf(device))
    }

    override suspend fun deleteDevice(id: String) {
        localDataSource.deleteDevice(id)
        // TODO: Push delete to remote? RemoteUpdate doesn't support delete yet?
    }

    override fun getAllDeviceTypes(): Flow<List<DeviceType>> = localDataSource.getAllDeviceTypes()

    override fun getDeviceTypeById(id: String): Flow<DeviceType?> = localDataSource.getDeviceTypeById(id)

    override suspend fun addDeviceType(type: DeviceType) {
        localDataSource.addDeviceType(type)
        pushUpdate(deviceTypes = listOf(type))
    }

    override suspend fun updateDeviceType(type: DeviceType) {
        localDataSource.updateDeviceType(type)
        pushUpdate(deviceTypes = listOf(type))
    }

    override suspend fun deleteDeviceType(id: String) {
        localDataSource.deleteDeviceType(id)
    }

    override fun getEventsForDevice(deviceId: String): Flow<List<BatteryEvent>> = localDataSource.getEventsForDevice(deviceId)

    override fun getAllEvents(): Flow<List<BatteryEvent>> = localDataSource.getAllEvents()

    override fun getEventById(id: String): Flow<BatteryEvent?> = localDataSource.getEventById(id)

    override suspend fun addEvent(event: BatteryEvent) {
        localDataSource.addEvent(event)
        pushUpdate(events = listOf(event))
    }

    override suspend fun updateEvent(event: BatteryEvent) {
        localDataSource.updateEvent(event)
        pushUpdate(events = listOf(event))
    }

    override suspend fun deleteEvent(id: String) {
        localDataSource.deleteEvent(id)
    }

    private fun pushUpdate(
        deviceTypes: List<DeviceType> = emptyList(),
        devices: List<Device> = emptyList(),
        events: List<BatteryEvent> = emptyList(),
    ) {
        scope.launch {
            remoteDataSource.push(
                RemoteUpdate(
                    isFullSnapshot = false,
                    deviceTypes = deviceTypes,
                    devices = devices,
                    events = events,
                ),
            )
        }
    }
}
