package com.chriscartland.batterybutler.server.data.repository

import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(kotlin.time.ExperimentalTime::class)
class InMemoryDeviceRepositoryTest {
    @Test
    fun `addDevice broadcasts update`() =
        runTest {
            val repo = InMemoryDeviceRepository()
            val device = Device(
                id = "d1",
                name = "Test Device",
                typeId = "t1",
                batteryLastReplaced = Instant.fromEpochMilliseconds(0),
                lastUpdated = Instant.fromEpochMilliseconds(0),
            )

            val updates = mutableListOf<RemoteUpdate>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                repo.getUpdates().collect { updates.add(it) }
            }

            repo.addDevice(device)

            // Expect 2 updates: 1. Initial Snapshot (DemoData), 2. New Snapshot (DemoData + New Device)
            assertEquals(2, updates.size)
            assertTrue(updates[1].isFullSnapshot)

            // Check that our device is in the list
            val foundDevice = updates[1].devices.find { it.id == "d1" }
            assertTrue(foundDevice != null)
            assertEquals("Test Device", foundDevice.name)
        }
}
