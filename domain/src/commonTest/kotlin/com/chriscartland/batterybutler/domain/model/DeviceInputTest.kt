package com.chriscartland.batterybutler.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DeviceInputTest {
    @Test
    fun `valid creation succeeds`() {
        val input = DeviceInput(
            name = "Smoke Detector",
            location = "Kitchen",
            typeId = "type-1",
        )

        assertEquals("Smoke Detector", input.name)
        assertEquals("Kitchen", input.location)
        assertEquals("type-1", input.typeId)
    }

    @Test
    fun `blank name throws`() {
        assertFailsWith<IllegalArgumentException> {
            DeviceInput(name = "", location = null, typeId = "type-1")
        }
    }

    @Test
    fun `whitespace-only name throws`() {
        assertFailsWith<IllegalArgumentException> {
            DeviceInput(name = "   ", location = null, typeId = "type-1")
        }
    }

    @Test
    fun `name at max length succeeds`() {
        val maxName = "a".repeat(DeviceInput.MAX_NAME_LENGTH)
        val input = DeviceInput(name = maxName, location = null, typeId = "type-1")

        assertEquals(100, input.name.length)
    }

    @Test
    fun `name exceeding max length throws`() {
        val tooLong = "a".repeat(DeviceInput.MAX_NAME_LENGTH + 1)
        assertFailsWith<IllegalArgumentException> {
            DeviceInput(name = tooLong, location = null, typeId = "type-1")
        }
    }

    @Test
    fun `blank typeId throws`() {
        assertFailsWith<IllegalArgumentException> {
            DeviceInput(name = "Valid", location = null, typeId = "")
        }
    }

    @Test
    fun `location at max length succeeds`() {
        val maxLocation = "b".repeat(DeviceInput.MAX_LOCATION_LENGTH)
        val input = DeviceInput(name = "Device", location = maxLocation, typeId = "type-1")

        assertEquals(100, input.location?.length)
    }

    @Test
    fun `location exceeding max length throws`() {
        val tooLong = "b".repeat(DeviceInput.MAX_LOCATION_LENGTH + 1)
        assertFailsWith<IllegalArgumentException> {
            DeviceInput(name = "Device", location = tooLong, typeId = "type-1")
        }
    }

    @Test
    fun `null location allowed`() {
        val input = DeviceInput(name = "Device", location = null, typeId = "type-1")

        assertNull(input.location)
    }
}
