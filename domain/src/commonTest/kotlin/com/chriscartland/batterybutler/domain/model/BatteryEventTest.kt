package com.chriscartland.batterybutler.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class BatteryEventTest {
    @Test
    fun `batteryEvent creation with all fields`() {
        val event = BatteryEvent(
            id = "event-1",
            deviceId = "device-1",
            date = Instant.parse("2023-06-15T10:30:00Z"),
            batteryType = "AA",
            notes = "Replaced with Duracell batteries",
        )

        assertEquals("event-1", event.id)
        assertEquals("device-1", event.deviceId)
        assertEquals(Instant.parse("2023-06-15T10:30:00Z"), event.date)
        assertEquals("AA", event.batteryType)
        assertEquals("Replaced with Duracell batteries", event.notes)
    }

    @Test
    fun `batteryEvent creation with optional fields null`() {
        val event = BatteryEvent(
            id = "event-1",
            deviceId = "device-1",
            date = Instant.DISTANT_PAST,
        )

        assertEquals("event-1", event.id)
        assertEquals("device-1", event.deviceId)
        assertNull(event.batteryType)
        assertNull(event.notes)
    }

    @Test
    fun `batteryEvent equals and hashCode work correctly`() {
        val timestamp = Instant.parse("2023-06-15T10:30:00Z")
        val event1 = BatteryEvent(
            id = "event-1",
            deviceId = "device-1",
            date = timestamp,
            batteryType = "AA",
        )
        val event2 = BatteryEvent(
            id = "event-1",
            deviceId = "device-1",
            date = timestamp,
            batteryType = "AA",
        )

        assertEquals(event1, event2)
        assertEquals(event1.hashCode(), event2.hashCode())
    }

    @Test
    fun `batteryEvent with different dates`() {
        val date1 = Instant.parse("2023-01-01T00:00:00Z")
        val date2 = Instant.parse("2023-12-31T23:59:59Z")

        val event1 = BatteryEvent(id = "1", deviceId = "d1", date = date1)
        val event2 = BatteryEvent(id = "2", deviceId = "d1", date = date2)

        assertEquals(date1, event1.date)
        assertEquals(date2, event2.date)
    }
}
