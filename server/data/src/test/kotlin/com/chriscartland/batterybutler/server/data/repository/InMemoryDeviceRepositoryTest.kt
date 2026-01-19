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

            assertEquals(1, updates.size)
            assertTrue(updates[0].isFullSnapshot)
            assertEquals(1, updates[0].devices.size)
            assertEquals("Test Device", updates[0].devices[0].name)
        }
}
