package com.chriscartland.batterybutler.server.app.repository

import com.chriscartland.batterybutler.domain.model.Device
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.time.toKotlinInstant

@kotlin.OptIn(kotlin.time.ExperimentalTime::class)
class PostgresDeviceRepositoryTest {
    @Before
    fun setup() {
        // Use in-memory H2 database for testing
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

        transaction {
            SchemaUtils.create(Devices, DeviceTypes, BatteryEvents)
            // Clean up before test if reusing DB instance
            SchemaUtils.drop(Devices, DeviceTypes, BatteryEvents)
            SchemaUtils.create(Devices, DeviceTypes, BatteryEvents)
        }
    }

    @Test
    fun `test add and get device`() =
        runBlocking {
            val repository = PostgresDeviceRepository()
            // Use Java Instant and convert to Kotlin Instant
            val now = Instant.now().toKotlinInstant()
            val device = Device(
                id = "device-1",
                name = "Test Device",
                typeId = "type-1",
                batteryLastReplaced = now,
                lastUpdated = now,
            )

            repository.addDevice(device)

            val devices = repository.getAllDevices().first()
            assertEquals(1, devices.size)
            assertEquals("device-1", devices[0].id)
            assertEquals("Test Device", devices[0].name)
        }

    @Test
    fun `test update device`() =
        runBlocking {
            val repository = PostgresDeviceRepository()
            val now = Instant.now().toKotlinInstant()
            val device = Device("d1", "Old Name", "t1", now, now)
            repository.addDevice(device)

            val updated = device.copy(name = "New Name")
            repository.updateDevice(updated)

            val devices = repository.getAllDevices().first()
            assertEquals("New Name", devices[0].name)
        }

    @Test
    fun `test delete device`() =
        runBlocking {
            val repository = PostgresDeviceRepository()
            val now = Instant.now().toKotlinInstant()
            repository.addDevice(Device("d1", "Name", "t1", now, now))
            assertEquals(1, repository.getAllDevices().first().size)

            repository.deleteDevice("d1")
            assertTrue(repository.getAllDevices().first().isEmpty())
        }
}
