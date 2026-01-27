package com.chriscartland.batterybutler.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class DeviceTest {
    @OptIn(ExperimentalTime::class)
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
        assertEquals("Kitchen", device.location)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `device creation fails with blank name`() {
        assertFailsWith<IllegalArgumentException> {
            Device(
                id = "1",
                name = "",
                typeId = "type-1",
                batteryLastReplaced = Instant.parse("2023-01-01T00:00:00Z"),
                lastUpdated = Instant.parse("2023-01-01T00:00:00Z"),
                location = "Kitchen",
            )
        }
    }
}
