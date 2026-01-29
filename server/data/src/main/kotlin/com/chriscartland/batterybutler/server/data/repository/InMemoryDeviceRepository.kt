package com.chriscartland.batterybutler.server.data.repository

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import com.chriscartland.batterybutler.fixtures.DemoData
import com.chriscartland.batterybutler.server.domain.repository.ServerDeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap

@OptIn(kotlin.time.ExperimentalTime::class)
class InMemoryDeviceRepository : ServerDeviceRepository {
    private val deviceTypes: ConcurrentHashMap<String, DeviceType>
    private val devices: ConcurrentHashMap<String, Device>
    private val events: ConcurrentHashMap<String, BatteryEvent>

    private val updates: MutableStateFlow<RemoteUpdate>

    init {
        Logger.d("InMemoryDeviceRepository") { "Initializing" }
        // Initialize with deterministic DemoData
        val initialDeviceTypes = DemoData.getDefaultDeviceTypes()
        val serverLabel = System.getenv("SERVER_LABEL") ?: "AWS Cloud"
        val initialDevices = DemoData.getDefaultDevices(initialDeviceTypes)
        val initialEvents = DemoData.getDefaultEvents(initialDevices)

        val renamedDevices = initialDevices.map {
            it.copy(name = "${it.name} [$serverLabel]")
        }

        deviceTypes = ConcurrentHashMap(initialDeviceTypes.associateBy { it.id })
        devices = ConcurrentHashMap(renamedDevices.associateBy { it.id })
        events = ConcurrentHashMap(initialEvents.associateBy { it.id })

        // Initialize StateFlow with the initial snapshot
        updates = MutableStateFlow(
            RemoteUpdate(
                isFullSnapshot = true,
                deviceTypes = initialDeviceTypes,
                devices = renamedDevices,
                events = initialEvents,
            ),
        )
    }

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

    override fun getUpdates(): Flow<RemoteUpdate> = updates.asStateFlow()

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
