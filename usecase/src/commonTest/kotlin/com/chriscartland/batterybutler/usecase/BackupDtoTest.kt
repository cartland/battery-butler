package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class BackupDtoTest {
    @Test
    fun `toDto and toDomain round-trip for Device`() {
        val original = Device(
            id = "device-1",
            name = "Smoke Detector",
            typeId = "type-1",
            batteryLastReplaced = Instant.parse("2024-06-15T10:30:00Z"),
            lastUpdated = Instant.parse("2024-06-15T10:30:00Z"),
            location = "Kitchen",
        )

        val dto = original.toDto()
        val restored = dto.toDomain()

        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertEquals(original.typeId, restored.typeId)
        assertEquals(original.batteryLastReplaced, restored.batteryLastReplaced)
        assertEquals(original.location, restored.location)
    }

    @Test
    fun `toDto and toDomain round-trip for DeviceType`() {
        val original = DeviceType(
            id = "type-1",
            name = "Smoke Detector",
            batteryType = "9V",
            batteryQuantity = 2,
            defaultIcon = "detector_smoke",
        )

        val dto = original.toDto()
        val restored = dto.toDomain()

        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertEquals(original.batteryType, restored.batteryType)
        assertEquals(original.batteryQuantity, restored.batteryQuantity)
        assertEquals(original.defaultIcon, restored.defaultIcon)
    }

    @Test
    fun `toDto and toDomain round-trip for BatteryEvent`() {
        val original = BatteryEvent(
            id = "event-1",
            deviceId = "device-1",
            date = Instant.parse("2024-06-20T14:00:00Z"),
            batteryType = "AA",
            notes = "Routine replacement",
        )

        val dto = original.toDto()
        val restored = dto.toDomain()

        assertEquals(original.id, restored.id)
        assertEquals(original.deviceId, restored.deviceId)
        assertEquals(original.date, restored.date)
        assertEquals(original.batteryType, restored.batteryType)
        assertEquals(original.notes, restored.notes)
    }

    @Test
    fun `toDomain handles null optional fields`() {
        val deviceDto = DeviceDto(
            id = "d1",
            name = "Test",
            typeId = "t1",
            batteryLastReplaced = null,
            location = null,
        )
        val device = deviceDto.toDomain()
        assertEquals(Instant.DISTANT_PAST, device.batteryLastReplaced)
        assertNull(device.location)

        val typeDto = DeviceTypeDto(
            id = "t1",
            name = "Type",
            batteryType = null,
            batteryQuantity = 1,
            defaultIcon = null,
        )
        val type = typeDto.toDomain()
        assertEquals("AA", type.batteryType) // defaults to AA
        assertNull(type.defaultIcon)

        val eventDto = BatteryEventDto(
            id = "e1",
            deviceId = "d1",
            date = "2024-01-01T00:00:00Z",
            batteryType = null,
            notes = null,
        )
        val event = eventDto.toDomain()
        assertNull(event.batteryType)
        assertNull(event.notes)
    }
}
