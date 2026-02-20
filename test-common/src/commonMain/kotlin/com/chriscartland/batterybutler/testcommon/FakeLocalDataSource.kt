package com.chriscartland.batterybutler.testcommon

import com.chriscartland.batterybutler.datalocal.LocalDataSource
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Fake implementation of [LocalDataSource] for testing.
 *
 * Tracks all mutation calls for verification and provides
 * controllable Flow-based getters.
 *
 * Example usage:
 * ```kotlin
 * val local = FakeLocalDataSource()
 * local.setDevicesForFlow(listOf(device1))
 * val repo = DefaultDeviceRepository(local, remote, scope)
 * ```
 */
class FakeLocalDataSource : LocalDataSource {
    val devices = mutableListOf<Device>()
    val updatedDevices = mutableListOf<Device>()
    val deletedDeviceIds = mutableListOf<String>()
    val deviceTypes = mutableListOf<DeviceType>()
    val updatedDeviceTypes = mutableListOf<DeviceType>()
    val deletedDeviceTypeIds = mutableListOf<String>()
    val events = mutableListOf<BatteryEvent>()
    val updatedEvents = mutableListOf<BatteryEvent>()
    val deletedEventIds = mutableListOf<String>()

    private var devicesFlow: Flow<List<Device>> = emptyFlow()
    private var deviceTypesFlow: Flow<List<DeviceType>> = emptyFlow()
    private var eventsFlow: Flow<List<BatteryEvent>> = emptyFlow()

    fun setDevicesForFlow(devices: List<Device>) {
        devicesFlow = flowOf(devices)
    }

    fun setDeviceTypesForFlow(types: List<DeviceType>) {
        deviceTypesFlow = flowOf(types)
    }

    fun setEventsForFlow(events: List<BatteryEvent>) {
        eventsFlow = flowOf(events)
    }

    override fun getAllDevices(): Flow<List<Device>> = devicesFlow

    override fun getDeviceById(id: String): Flow<Device?> = emptyFlow()

    override suspend fun addDevice(device: Device) {
        devices.add(device)
    }

    override suspend fun addDevices(devices: List<Device>) {
        this.devices.addAll(devices)
    }

    override suspend fun updateDevice(device: Device) {
        updatedDevices.add(device)
    }

    override suspend fun deleteDevice(id: String) {
        deletedDeviceIds.add(id)
    }

    override fun getAllDeviceTypes(): Flow<List<DeviceType>> = deviceTypesFlow

    override fun getDeviceTypeById(id: String): Flow<DeviceType?> = emptyFlow()

    override suspend fun addDeviceType(type: DeviceType) {
        deviceTypes.add(type)
    }

    override suspend fun addDeviceTypes(types: List<DeviceType>) {
        deviceTypes.addAll(types)
    }

    override suspend fun updateDeviceType(type: DeviceType) {
        updatedDeviceTypes.add(type)
    }

    override suspend fun deleteDeviceType(id: String) {
        deletedDeviceTypeIds.add(id)
    }

    override fun getEventsForDevice(deviceId: String): Flow<List<BatteryEvent>> = emptyFlow()

    override fun getAllEvents(): Flow<List<BatteryEvent>> = eventsFlow

    override fun getEventById(id: String): Flow<BatteryEvent?> = emptyFlow()

    override suspend fun addEvent(event: BatteryEvent) {
        events.add(event)
    }

    override suspend fun addEvents(events: List<BatteryEvent>) {
        this.events.addAll(events)
    }

    override suspend fun updateEvent(event: BatteryEvent) {
        updatedEvents.add(event)
    }

    override suspend fun deleteEvent(id: String) {
        deletedEventIds.add(id)
    }
}
