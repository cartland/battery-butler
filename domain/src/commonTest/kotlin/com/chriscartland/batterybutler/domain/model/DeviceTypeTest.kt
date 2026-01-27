package com.chriscartland.batterybutler.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeviceTypeTest {
    @Test
    fun `deviceType creation with all fields`() {
        val type = DeviceType(
            id = "type-1",
            name = "Smoke Detector",
            defaultIcon = "detector_smoke",
            batteryType = "9V",
            batteryQuantity = 2,
        )

        assertEquals("type-1", type.id)
        assertEquals("Smoke Detector", type.name)
        assertEquals("detector_smoke", type.defaultIcon)
        assertEquals("9V", type.batteryType)
        assertEquals(2, type.batteryQuantity)
    }

    @Test
    fun `deviceType creation with default values`() {
        val type = DeviceType(
            id = "type-1",
            name = "Generic Device",
        )

        assertEquals("type-1", type.id)
        assertEquals("Generic Device", type.name)
        assertNull(type.defaultIcon)
        assertEquals("AA", type.batteryType)
        assertEquals(1, type.batteryQuantity)
    }

    @Test
    fun `deviceType equals and hashCode work correctly`() {
        val type1 = DeviceType(
            id = "type-1",
            name = "Test Type",
            batteryType = "AAA",
            batteryQuantity = 3,
        )
        val type2 = DeviceType(
            id = "type-1",
            name = "Test Type",
            batteryType = "AAA",
            batteryQuantity = 3,
        )

        assertEquals(type1, type2)
        assertEquals(type1.hashCode(), type2.hashCode())
    }

    @Test
    fun `deviceType with different battery configurations`() {
        val aaType = DeviceType(id = "1", name = "Remote", batteryType = "AA", batteryQuantity = 2)
        val aaaType = DeviceType(id = "2", name = "Mouse", batteryType = "AAA", batteryQuantity = 1)
        val nineVType = DeviceType(id = "3", name = "Smoke Detector", batteryType = "9V", batteryQuantity = 1)

        assertEquals("AA", aaType.batteryType)
        assertEquals(2, aaType.batteryQuantity)
        assertEquals("AAA", aaaType.batteryType)
        assertEquals("9V", nineVType.batteryType)
    }
}
