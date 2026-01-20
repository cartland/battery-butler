package com.chriscartland.batterybutler.networking

import com.chriscartland.batterybutler.domain.demo.DemoData
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.repository.RemoteDataSource
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapNotNull
import me.tatarka.inject.annotations.Inject

@Inject
class MockRemoteDataSource : RemoteDataSource {
    private val state = MutableStateFlow<RemoteUpdate?>(null)

    private val deviceTypes = DemoData.getDefaultDeviceTypes().toMutableList()
    private val devices = mutableListOf<Device>()
    private val events = mutableListOf<BatteryEvent>()

    init {
        // Initial snapshot
        emitSnapshot()
    }

    private fun emitSnapshot() {
        state.value = RemoteUpdate(
            isFullSnapshot = true,
            deviceTypes = deviceTypes.toList(),
            devices = devices.toList(),
            events = events.toList(),
        )
    }

    override fun subscribe(): Flow<RemoteUpdate> = state.asStateFlow().mapNotNull { it }

    override suspend fun push(update: RemoteUpdate): Boolean {
        // Simulate network processing by updating in-memory state
        // In a real sync, we'd handle conflicts, but here we just append/replace

        // Naive implementation:
        // 1. Device Types
        update.deviceTypes.forEach { newType ->
            val index = deviceTypes.indexOfFirst { it.id == newType.id }
            if (index != -1) {
                deviceTypes[index] = newType
            } else {
                deviceTypes.add(newType)
            }
        }

        // 2. Devices
        update.devices.forEach { newDevice ->
            val index = devices.indexOfFirst { it.id == newDevice.id }
            if (index != -1) {
                devices[index] = newDevice
            } else {
                devices.add(newDevice)
            }
        }

        // 3. Events
        update.events.forEach { newEvent ->
            val index = events.indexOfFirst { it.id == newEvent.id }
            if (index != -1) {
                events[index] = newEvent
            } else {
                events.add(newEvent)
            }
        }

        // Emit new state as partial or full?
        // Mock usually reflects what happened. Let's send a full snapshot back to be safe for now,
        // or just echo the update if we want to simulate "Server ack".
        // But the requirement says "update the network results in-memory".
        // The UI/Repo subscribes to "subscribe()".

        emitSnapshot()

        return true
    }
}
