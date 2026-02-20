package com.chriscartland.batterybutler.datalocal.room.entity

import com.chriscartland.batterybutler.testcommon.TestDevices
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class EntityMapperTest {
    // ── DeviceEntity ↔ Device ────────────────────────────────────────

    @Test
    fun `Device round-trip preserves all fields`() {
        val device = TestDevices.createDevice(
            id = "dev-1",
            name = "Smoke Detector",
            typeId = "type-smoke",
            batteryLastReplaced = Instant.parse("2024-06-15T10:30:00Z"),
            lastUpdated = Instant.parse("2024-07-01T08:00:00Z"),
            location = "Kitchen",
            imagePath = "/images/smoke.png",
        )

        val result = device.toEntity().toDomain()

        assertEquals(device, result)
    }

    @Test
    fun `Device round-trip with null location and imagePath`() {
        val device = TestDevices.createDevice(
            batteryLastReplaced = Instant.parse("2024-01-01T00:00:00Z"),
            lastUpdated = Instant.parse("2024-01-01T00:00:00Z"),
            location = null,
            imagePath = null,
        )

        val result = device.toEntity().toDomain()

        assertEquals(device, result)
        assertNull(result.location)
        assertNull(result.imagePath)
    }

    @Test
    fun `DeviceEntity toDomain maps timestamps correctly`() {
        val epochMillis = 1718448600000L // 2024-06-15T10:30:00Z
        val entity = DeviceEntity(
            id = "dev-1",
            name = "Test",
            typeId = "type-1",
            batteryLastReplaced = epochMillis,
            lastUpdated = epochMillis,
            location = null,
            imagePath = null,
        )

        val domain = entity.toDomain()

        assertEquals(epochMillis, domain.batteryLastReplaced.toEpochMilliseconds())
        assertEquals(epochMillis, domain.lastUpdated.toEpochMilliseconds())
    }

    @Test
    fun `Device toEntity maps timestamps to epoch millis`() {
        val instant = Instant.parse("2024-06-15T10:30:00Z")
        val device = TestDevices.createDevice(
            batteryLastReplaced = instant,
            lastUpdated = instant,
        )

        val entity = device.toEntity()

        assertEquals(instant.toEpochMilliseconds(), entity.batteryLastReplaced)
        assertEquals(instant.toEpochMilliseconds(), entity.lastUpdated)
    }

    // ── DeviceTypeEntity ↔ DeviceType ────────────────────────────────

    @Test
    fun `DeviceType round-trip preserves all fields`() {
        val deviceType = TestDevices.createDeviceType(
            id = "type-1",
            name = "Thermostat",
            defaultIcon = "thermostat",
            batteryType = "CR2032",
            batteryQuantity = 1,
        )

        val result = deviceType.toEntity().toDomain()

        assertEquals(deviceType, result)
    }

    @Test
    fun `DeviceType round-trip with null defaultIcon`() {
        val deviceType = TestDevices.createDeviceType(
            defaultIcon = null,
        )

        val result = deviceType.toEntity().toDomain()

        assertEquals(deviceType, result)
        assertNull(result.defaultIcon)
    }

    @Test
    fun `DeviceType round-trip preserves default batteryType and batteryQuantity`() {
        val entity = DeviceTypeEntity(
            id = "type-default",
            name = "Default Type",
            defaultIcon = null,
        )

        val domain = entity.toDomain()

        assertEquals("AA", domain.batteryType)
        assertEquals(1, domain.batteryQuantity)

        val roundTripped = domain.toEntity()
        assertEquals(entity, roundTripped)
    }

    // ── BatteryEventEntity ↔ BatteryEvent ────────────────────────────

    @Test
    fun `BatteryEvent round-trip preserves all fields`() {
        val event = TestDevices.createBatteryEvent(
            id = "evt-1",
            deviceId = "dev-1",
            date = Instant.parse("2024-06-15T10:30:00Z"),
            batteryType = "9V",
            notes = "Replaced during inspection",
        )

        val result = event.toEntity().toDomain()

        assertEquals(event, result)
    }

    @Test
    fun `BatteryEvent round-trip with null notes and batteryType`() {
        val event = TestDevices.createBatteryEvent(
            date = Instant.parse("2024-01-01T00:00:00Z"),
            batteryType = null,
            notes = null,
        )

        val result = event.toEntity().toDomain()

        assertEquals(event, result)
        assertNull(result.notes)
        assertNull(result.batteryType)
    }

    @Test
    fun `BatteryEventEntity toDomain maps date timestamp correctly`() {
        val epochMillis = 1718448600000L
        val entity = BatteryEventEntity(
            id = "evt-1",
            deviceId = "dev-1",
            date = epochMillis,
            batteryType = "AA",
            notes = null,
        )

        val domain = entity.toDomain()

        assertEquals(epochMillis, domain.date.toEpochMilliseconds())
    }

    // ── Timestamp edge cases ─────────────────────────────────────────

    @Test
    fun `zero timestamp round-trips correctly`() {
        val device = TestDevices.createDevice(
            batteryLastReplaced = Instant.fromEpochMilliseconds(0),
            lastUpdated = Instant.fromEpochMilliseconds(0),
        )

        val entity = device.toEntity()
        assertEquals(0L, entity.batteryLastReplaced)
        assertEquals(0L, entity.lastUpdated)

        val roundTripped = entity.toDomain()
        assertEquals(device, roundTripped)
    }

    @Test
    fun `negative timestamp round-trips correctly`() {
        val negativeInstant = Instant.fromEpochMilliseconds(-86400000L) // 1 day before epoch
        val event = TestDevices.createBatteryEvent(
            date = negativeInstant,
        )

        val entity = event.toEntity()
        assertEquals(-86400000L, entity.date)

        val roundTripped = entity.toDomain()
        assertEquals(event, roundTripped)
    }
}
