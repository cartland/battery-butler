package com.chriscartland.batterybutler.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class DeviceTest {
    @Test
    fun `device creation with valid data`() {
        val device = Device(
            id = "1",
            name = "Smoke Detector",
            typeId = "type-1",
            batteryLastReplaced = Instant.parse("2023-01-01T00:00:00Z"),
            lastUpdated = Instant.parse("2023-01-01T00:00:00Z"),
            location = "Kitchen",
        )

        assertEquals("1", device.id)
        assertEquals("Smoke Detector", device.name)
        assertEquals("type-1", device.typeId)
        assertEquals("Kitchen", device.location)
    }

    @Test
    fun `device creation with optional fields null`() {
        val device = Device(
            id = "1",
            name = "Test Device",
            typeId = "type-1",
            batteryLastReplaced = Instant.DISTANT_PAST,
            lastUpdated = Instant.DISTANT_PAST,
        )

        assertNull(device.location)
        assertNull(device.imagePath)
    }

    @Test
    fun `device creation fails with blank name`() {
        assertFailsWith<IllegalArgumentException> {
            Device(
                id = "1",
                name = "",
                typeId = "type-1",
                batteryLastReplaced = Instant.parse("2023-01-01T00:00:00Z"),
                lastUpdated = Instant.parse("2023-01-01T00:00:00Z"),
            )
        }
    }

    @Test
    fun `device creation fails with whitespace-only name`() {
        assertFailsWith<IllegalArgumentException> {
            Device(
                id = "1",
                name = "   ",
                typeId = "type-1",
                batteryLastReplaced = Instant.DISTANT_PAST,
                lastUpdated = Instant.DISTANT_PAST,
            )
        }
    }

    @Test
    fun `device creation fails with blank id`() {
        assertFailsWith<IllegalArgumentException> {
            Device(
                id = "",
                name = "Valid Name",
                typeId = "type-1",
                batteryLastReplaced = Instant.DISTANT_PAST,
                lastUpdated = Instant.DISTANT_PAST,
            )
        }
    }

    @Test
    fun `device creation fails with whitespace-only id`() {
        assertFailsWith<IllegalArgumentException> {
            Device(
                id = "   ",
                name = "Valid Name",
                typeId = "type-1",
                batteryLastReplaced = Instant.DISTANT_PAST,
                lastUpdated = Instant.DISTANT_PAST,
            )
        }
    }

    @Test
    fun `device equals and hashCode work correctly`() {
        val timestamp = Instant.parse("2023-01-01T00:00:00Z")
        val device1 = Device(
            id = "1",
            name = "Test",
            typeId = "type-1",
            batteryLastReplaced = timestamp,
            lastUpdated = timestamp,
        )
        val device2 = Device(
            id = "1",
            name = "Test",
            typeId = "type-1",
            batteryLastReplaced = timestamp,
            lastUpdated = timestamp,
        )

        assertEquals(device1, device2)
        assertEquals(device1.hashCode(), device2.hashCode())
    }
}
