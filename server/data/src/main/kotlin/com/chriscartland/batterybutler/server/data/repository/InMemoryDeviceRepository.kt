package com.chriscartland.batterybutler.server.data.repository

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import com.chriscartland.batterybutler.server.domain.repository.ServerDeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap

class InMemoryDeviceRepository : ServerDeviceRepository {
    private val devices = ConcurrentHashMap<String, Device>()
    private val deviceTypes = ConcurrentHashMap<String, DeviceType>()
    private val events = ConcurrentHashMap<String, BatteryEvent>()

    private val updates = MutableSharedFlow<RemoteUpdate>(replay = 0)

    override fun getAllDevices(): Flow<List<Device>> = flow { emit(devices.values.toList()) }

    override fun getAllDeviceTypes(): Flow<List<DeviceType>> = flow { emit(deviceTypes.values.toList()) }

    override fun getAllEvents(): Flow<List<BatteryEvent>> = flow { emit(events.values.toList()) }

    override suspend fun addDevice(device: Device) {
        devices[device.id] = device
        broadcastUpdate()
    }

    override suspend fun updateDevice(device: Device) {
        devices[device.id] = device
        broadcastUpdate()
    }

    override suspend fun deleteDevice(id: String) {
        devices.remove(id)
        broadcastUpdate()
    }

    override suspend fun addDeviceType(type: DeviceType) {
        deviceTypes[type.id] = type
        broadcastUpdate()
    }

    override suspend fun updateDeviceType(type: DeviceType) {
        deviceTypes[type.id] = type
        broadcastUpdate()
    }

    override suspend fun deleteDeviceType(id: String) {
        deviceTypes.remove(id)
        broadcastUpdate()
    }

    override suspend fun addEvent(event: BatteryEvent) {
        events[event.id] = event
        broadcastUpdate()
    }

    override suspend fun deleteEvent(id: String) {
        events.remove(id)
        broadcastUpdate()
    }

    override fun getUpdates(): Flow<RemoteUpdate> = updates.asSharedFlow()

    private suspend fun broadcastUpdate() {
        // MVP: Send full snapshot on every update
        val snapshot = RemoteUpdate(
            isFullSnapshot = true,
            deviceTypes = deviceTypes.values.toList(),
            devices = devices.values.toList(),
            events = events.values.toList(),
        )
        updates.emit(snapshot)
    }
}
