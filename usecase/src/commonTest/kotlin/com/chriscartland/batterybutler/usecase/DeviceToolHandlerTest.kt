package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.ai.AiToolNames
import com.chriscartland.batterybutler.domain.model.ai.AiToolParams
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class DeviceToolHandlerTest {
    private fun createHandler(
        repo: FakeDeviceRepository = FakeDeviceRepository(),
    ): Pair<DeviceToolHandler, FakeDeviceRepository> {
        val findOrCreateType = FindOrCreateDeviceTypeUseCase(repo)
        val findOrCreateDevice = FindOrCreateDeviceUseCase(repo, findOrCreateType)
        val updateLastReplaced = UpdateDeviceLastReplacedUseCase(repo)
        val handler = DeviceToolHandler(repo, findOrCreateType, findOrCreateDevice, updateLastReplaced)
        return handler to repo
    }

    // region addDevice

    @Test
    fun `addDevice with name creates device with default type`() =
        runTest {
            val (handler, repo) = createHandler()

            val result = handler.execute(
                AiToolNames.ADD_DEVICE,
                mapOf(AiToolParams.NAME to "Smoke Detector"),
            )

            assertTrue(result.startsWith("Success:"))
            assertTrue(result.contains("Smoke Detector"))
            assertEquals(1, repo.devices.size)
            assertEquals("Smoke Detector", repo.devices[0].name)
            assertEquals("default_type", repo.devices[0].typeId)
        }

    @Test
    fun `addDevice with existing type name reuses existing type ID`() =
        runTest {
            val repo = FakeDeviceRepository()
            val existingType = TestDevices.createDeviceType(id = "type-123", name = "Thermostat")
            repo.setDeviceTypes(listOf(existingType))
            val (handler, _) = createHandler(repo)

            val result = handler.execute(
                AiToolNames.ADD_DEVICE,
                mapOf(AiToolParams.NAME to "Living Room Thermostat", AiToolParams.TYPE to "Thermostat"),
            )

            assertTrue(result.startsWith("Success:"))
            assertEquals(1, repo.devices.size)
            assertEquals("type-123", repo.devices[0].typeId)
            // No new device type should have been created
            assertEquals(1, repo.deviceTypes.size)
        }

    @Test
    fun `addDevice with new type name creates type first then device`() =
        runTest {
            val (handler, repo) = createHandler()

            val result = handler.execute(
                AiToolNames.ADD_DEVICE,
                mapOf(AiToolParams.NAME to "Smart Lock", AiToolParams.TYPE to "Lock"),
            )

            assertTrue(result.startsWith("Success:"))
            assertEquals(1, repo.devices.size)
            assertEquals(1, repo.deviceTypes.size)
            assertEquals("Lock", repo.deviceTypes[0].name)
            assertEquals("default", repo.deviceTypes[0].defaultIcon)
            // Device should reference the newly created type
            assertEquals(repo.deviceTypes[0].id, repo.devices[0].typeId)
        }

    @Test
    fun `addDevice missing name returns error`() =
        runTest {
            val (handler, repo) = createHandler()

            val result = handler.execute(AiToolNames.ADD_DEVICE, emptyMap())

            assertEquals("Error: Missing name", result)
            assertTrue(repo.devices.isEmpty())
        }

    @Test
    fun `addDevice with blank type uses default type`() =
        runTest {
            val (handler, repo) = createHandler()

            val result = handler.execute(
                AiToolNames.ADD_DEVICE,
                mapOf(AiToolParams.NAME to "Remote", AiToolParams.TYPE to ""),
            )

            assertTrue(result.startsWith("Success:"))
            assertEquals("default_type", repo.devices[0].typeId)
        }

    // endregion

    // region addDeviceType

    @Test
    fun `addDeviceType with all params creates type with given values`() =
        runTest {
            val (handler, repo) = createHandler()

            val result = handler.execute(
                AiToolNames.ADD_DEVICE_TYPE,
                mapOf(
                    AiToolParams.NAME to "Smoke Alarm",
                    AiToolParams.BATTERY_TYPE to "9V",
                    AiToolParams.ICON to "detector_smoke",
                ),
            )

            assertTrue(result.startsWith("Success:"))
            assertTrue(result.contains("Smoke Alarm"))
            assertTrue(result.contains("9V"))
            assertTrue(result.contains("detector_smoke"))
            assertEquals(1, repo.deviceTypes.size)
            assertEquals("Smoke Alarm", repo.deviceTypes[0].name)
            assertEquals("detector_smoke", repo.deviceTypes[0].defaultIcon)
        }

    @Test
    fun `addDeviceType missing name returns error`() =
        runTest {
            val (handler, repo) = createHandler()

            val result = handler.execute(AiToolNames.ADD_DEVICE_TYPE, emptyMap())

            assertEquals("Error: Missing name", result)
            assertTrue(repo.deviceTypes.isEmpty())
        }

    @Test
    fun `addDeviceType uses defaults for missing batteryType and icon`() =
        runTest {
            val (handler, repo) = createHandler()

            val result = handler.execute(
                AiToolNames.ADD_DEVICE_TYPE,
                mapOf(AiToolParams.NAME to "Generic"),
            )

            assertTrue(result.contains("Unknown"))
            assertTrue(result.contains("default"))
            assertEquals(1, repo.deviceTypes.size)
            assertEquals("default", repo.deviceTypes[0].defaultIcon)
        }

    // endregion

    // region recordBatteryReplacement

    @Test
    fun `recordBatteryReplacement with existing device adds event`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(
                id = "dev-1",
                name = "Kitchen Alarm",
                batteryLastReplaced = Instant.fromEpochMilliseconds(0),
            )
            repo.setDevices(listOf(device))
            val (handler, _) = createHandler(repo)

            val result = handler.execute(
                AiToolNames.RECORD_BATTERY_REPLACEMENT,
                mapOf(AiToolParams.DEVICE_NAME to "Kitchen Alarm", AiToolParams.DATE to "2024-06-15"),
            )

            assertTrue(result.startsWith("Success:"))
            assertTrue(result.contains("Kitchen Alarm"))
            assertTrue(result.contains("2024-06-15"))
            assertEquals(1, repo.events.size)
            assertEquals("dev-1", repo.events[0].deviceId)
            // Should not create a new device
            assertEquals(1, repo.devices.size)
        }

    @Test
    fun `recordBatteryReplacement with new device creates device first`() =
        runTest {
            val (handler, repo) = createHandler()

            val result = handler.execute(
                AiToolNames.RECORD_BATTERY_REPLACEMENT,
                mapOf(AiToolParams.DEVICE_NAME to "New Gadget", AiToolParams.DATE to "2024-03-01"),
            )

            assertTrue(result.startsWith("Success:"))
            assertEquals(1, repo.devices.size)
            assertEquals("New Gadget", repo.devices[0].name)
            assertEquals(1, repo.events.size)
            assertEquals(repo.devices[0].id, repo.events[0].deviceId)
        }

    @Test
    fun `recordBatteryReplacement updates device lastReplaced if newer`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(
                id = "dev-1",
                name = "Alarm",
                batteryLastReplaced = Instant.fromEpochMilliseconds(0),
            )
            repo.setDevices(listOf(device))
            val (handler, _) = createHandler(repo)

            handler.execute(
                AiToolNames.RECORD_BATTERY_REPLACEMENT,
                mapOf(AiToolParams.DEVICE_NAME to "Alarm", AiToolParams.DATE to "2024-06-15"),
            )

            val updatedDevice = repo.devices[0]
            assertTrue(updatedDevice.batteryLastReplaced > Instant.fromEpochMilliseconds(0))
        }

    @Test
    fun `recordBatteryReplacement does NOT update lastReplaced if older`() =
        runTest {
            val repo = FakeDeviceRepository()
            val recentInstant = Instant.parse("2025-01-01T00:00:00Z")
            val device = TestDevices.createDevice(
                id = "dev-1",
                name = "Alarm",
                batteryLastReplaced = recentInstant,
            )
            repo.setDevices(listOf(device))
            val (handler, _) = createHandler(repo)

            handler.execute(
                AiToolNames.RECORD_BATTERY_REPLACEMENT,
                mapOf(AiToolParams.DEVICE_NAME to "Alarm", AiToolParams.DATE to "2020-01-01"),
            )

            // batteryLastReplaced should remain the original (newer) value
            assertEquals(recentInstant, repo.devices[0].batteryLastReplaced)
        }

    @Test
    fun `recordBatteryReplacement missing deviceName returns error`() =
        runTest {
            val (handler, repo) = createHandler()

            val result = handler.execute(
                AiToolNames.RECORD_BATTERY_REPLACEMENT,
                mapOf(AiToolParams.DATE to "2024-06-15"),
            )

            assertEquals("Error: Missing deviceName", result)
            assertTrue(repo.events.isEmpty())
        }

    @Test
    fun `recordBatteryReplacement missing date returns error`() =
        runTest {
            val (handler, repo) = createHandler()

            val result = handler.execute(
                AiToolNames.RECORD_BATTERY_REPLACEMENT,
                mapOf(AiToolParams.DEVICE_NAME to "Alarm"),
            )

            assertEquals("Error: Missing date", result)
            assertTrue(repo.events.isEmpty())
        }

    @Test
    fun `recordBatteryReplacement invalid date format returns error`() =
        runTest {
            val (handler, repo) = createHandler()

            val result = handler.execute(
                AiToolNames.RECORD_BATTERY_REPLACEMENT,
                mapOf(AiToolParams.DEVICE_NAME to "Alarm", AiToolParams.DATE to "not-a-date"),
            )

            assertTrue(result.startsWith("Error executing"))
            assertTrue(repo.events.isEmpty())
        }

    @Test
    fun `recordBatteryReplacement with new device and type creates both`() =
        runTest {
            val (handler, repo) = createHandler()

            val result = handler.execute(
                AiToolNames.RECORD_BATTERY_REPLACEMENT,
                mapOf(
                    AiToolParams.DEVICE_NAME to "New Alarm",
                    AiToolParams.DATE to "2024-06-15",
                    AiToolParams.DEVICE_TYPE to "Smoke Alarm",
                ),
            )

            assertTrue(result.startsWith("Success:"))
            assertEquals(1, repo.devices.size)
            assertEquals(1, repo.deviceTypes.size)
            assertEquals("Smoke Alarm", repo.deviceTypes[0].name)
            assertEquals(repo.deviceTypes[0].id, repo.devices[0].typeId)
            assertEquals(1, repo.events.size)
        }

    // endregion

    // region updateDevice

    @Test
    fun `updateDevice updates name and location`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(id = "dev-1", name = "Old Name", location = "Kitchen")
            repo.setDevices(listOf(device))
            val (handler, _) = createHandler(repo)

            val result = handler.execute(
                AiToolNames.UPDATE_DEVICE,
                mapOf(
                    AiToolParams.ID to "dev-1",
                    AiToolParams.NEW_NAME to "New Name",
                    AiToolParams.NEW_LOCATION to "Living Room",
                ),
            )

            assertTrue(result.startsWith("Success:"))
            assertTrue(result.contains("New Name"))
            assertEquals("New Name", repo.devices[0].name)
            assertEquals("Living Room", repo.devices[0].location)
        }

    @Test
    fun `updateDevice with new type creates type and updates device`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(id = "dev-1", name = "Detector")
            repo.setDevices(listOf(device))
            val (handler, _) = createHandler(repo)

            val result = handler.execute(
                AiToolNames.UPDATE_DEVICE,
                mapOf(AiToolParams.ID to "dev-1", AiToolParams.NEW_TYPE to "Smoke Alarm"),
            )

            assertTrue(result.startsWith("Success:"))
            assertEquals(1, repo.deviceTypes.size)
            assertEquals("Smoke Alarm", repo.deviceTypes[0].name)
            assertEquals(repo.deviceTypes[0].id, repo.devices[0].typeId)
        }

    @Test
    fun `updateDevice missing id returns error`() =
        runTest {
            val (handler, _) = createHandler()

            val result = handler.execute(AiToolNames.UPDATE_DEVICE, emptyMap())

            assertEquals("Error: Missing id", result)
        }

    @Test
    fun `updateDevice nonexistent id returns error`() =
        runTest {
            val (handler, _) = createHandler()

            val result = handler.execute(
                AiToolNames.UPDATE_DEVICE,
                mapOf(AiToolParams.ID to "nonexistent"),
            )

            assertEquals("Error: Device not found with id 'nonexistent'", result)
        }

    @Test
    fun `updateDevice preserves unchanged fields`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(
                id = "dev-1",
                name = "Original",
                location = "Kitchen",
                typeId = "type-1",
            )
            repo.setDevices(listOf(device))
            val (handler, _) = createHandler(repo)

            handler.execute(
                AiToolNames.UPDATE_DEVICE,
                mapOf(AiToolParams.ID to "dev-1", AiToolParams.NEW_NAME to "Renamed"),
            )

            assertEquals("Renamed", repo.devices[0].name)
            assertEquals("Kitchen", repo.devices[0].location)
            assertEquals("type-1", repo.devices[0].typeId)
        }

    // endregion

    // region deleteDevice

    @Test
    fun `deleteDevice removes device and its events`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(id = "dev-1", name = "Alarm")
            val event1 = TestDevices.createBatteryEvent(id = "evt-1", deviceId = "dev-1")
            val event2 = TestDevices.createBatteryEvent(id = "evt-2", deviceId = "dev-1")
            repo.setDevices(listOf(device))
            repo.setEvents(listOf(event1, event2))
            val (handler, _) = createHandler(repo)

            val result = handler.execute(
                AiToolNames.DELETE_DEVICE,
                mapOf(AiToolParams.ID to "dev-1"),
            )

            assertTrue(result.startsWith("Success:"))
            assertTrue(result.contains("Alarm"))
            assertTrue(result.contains("2 associated event(s)"))
            assertTrue(repo.devices.isEmpty())
            assertTrue(repo.events.isEmpty())
        }

    @Test
    fun `deleteDevice missing id returns error`() =
        runTest {
            val (handler, _) = createHandler()

            val result = handler.execute(AiToolNames.DELETE_DEVICE, emptyMap())

            assertEquals("Error: Missing id", result)
        }

    @Test
    fun `deleteDevice nonexistent id returns error`() =
        runTest {
            val (handler, _) = createHandler()

            val result = handler.execute(
                AiToolNames.DELETE_DEVICE,
                mapOf(AiToolParams.ID to "nonexistent"),
            )

            assertEquals("Error: Device not found with id 'nonexistent'", result)
        }

    // endregion

    // region updateDeviceType

    @Test
    fun `updateDeviceType updates name and battery info`() =
        runTest {
            val repo = FakeDeviceRepository()
            val type = TestDevices.createDeviceType(
                id = "type-1",
                name = "Old Type",
                batteryType = "AA",
                batteryQuantity = 2,
            )
            repo.setDeviceTypes(listOf(type))
            val (handler, _) = createHandler(repo)

            val result = handler.execute(
                AiToolNames.UPDATE_DEVICE_TYPE,
                mapOf(
                    AiToolParams.ID to "type-1",
                    AiToolParams.NEW_NAME to "New Type",
                    AiToolParams.NEW_BATTERY_TYPE to "AAA",
                    AiToolParams.NEW_BATTERY_QUANTITY to 4,
                ),
            )

            assertTrue(result.startsWith("Success:"))
            assertEquals("New Type", repo.deviceTypes[0].name)
            assertEquals("AAA", repo.deviceTypes[0].batteryType)
            assertEquals(4, repo.deviceTypes[0].batteryQuantity)
        }

    @Test
    fun `updateDeviceType parses string battery quantity`() =
        runTest {
            val repo = FakeDeviceRepository()
            val type = TestDevices.createDeviceType(id = "type-1", name = "Test", batteryQuantity = 1)
            repo.setDeviceTypes(listOf(type))
            val (handler, _) = createHandler(repo)

            handler.execute(
                AiToolNames.UPDATE_DEVICE_TYPE,
                mapOf(AiToolParams.ID to "type-1", AiToolParams.NEW_BATTERY_QUANTITY to "3"),
            )

            assertEquals(3, repo.deviceTypes[0].batteryQuantity)
        }

    @Test
    fun `updateDeviceType missing id returns error`() =
        runTest {
            val (handler, _) = createHandler()

            val result = handler.execute(AiToolNames.UPDATE_DEVICE_TYPE, emptyMap())

            assertEquals("Error: Missing id", result)
        }

    @Test
    fun `updateDeviceType nonexistent id returns error`() =
        runTest {
            val (handler, _) = createHandler()

            val result = handler.execute(
                AiToolNames.UPDATE_DEVICE_TYPE,
                mapOf(AiToolParams.ID to "nonexistent"),
            )

            assertEquals("Error: Device type not found with id 'nonexistent'", result)
        }

    // endregion

    // region deleteDeviceType

    @Test
    fun `deleteDeviceType succeeds when no devices use it`() =
        runTest {
            val repo = FakeDeviceRepository()
            val type = TestDevices.createDeviceType(id = "type-1", name = "Unused Type")
            repo.setDeviceTypes(listOf(type))
            val (handler, _) = createHandler(repo)

            val result = handler.execute(
                AiToolNames.DELETE_DEVICE_TYPE,
                mapOf(AiToolParams.ID to "type-1"),
            )

            assertTrue(result.startsWith("Success:"))
            assertTrue(result.contains("Unused Type"))
            assertTrue(repo.deviceTypes.isEmpty())
        }

    @Test
    fun `deleteDeviceType fails when devices still use it`() =
        runTest {
            val repo = FakeDeviceRepository()
            val type = TestDevices.createDeviceType(id = "type-1", name = "Used Type")
            val device = TestDevices.createDevice(id = "dev-1", name = "Alarm", typeId = "type-1")
            repo.setDeviceTypes(listOf(type))
            repo.setDevices(listOf(device))
            val (handler, _) = createHandler(repo)

            val result = handler.execute(
                AiToolNames.DELETE_DEVICE_TYPE,
                mapOf(AiToolParams.ID to "type-1"),
            )

            assertTrue(result.startsWith("Error:"))
            assertTrue(result.contains("1 device(s) still use it"))
            // Type should not be deleted
            assertEquals(1, repo.deviceTypes.size)
        }

    @Test
    fun `deleteDeviceType missing id returns error`() =
        runTest {
            val (handler, _) = createHandler()

            val result = handler.execute(AiToolNames.DELETE_DEVICE_TYPE, emptyMap())

            assertEquals("Error: Missing id", result)
        }

    @Test
    fun `deleteDeviceType nonexistent id returns error`() =
        runTest {
            val (handler, _) = createHandler()

            val result = handler.execute(
                AiToolNames.DELETE_DEVICE_TYPE,
                mapOf(AiToolParams.ID to "nonexistent"),
            )

            assertEquals("Error: Device type not found with id 'nonexistent'", result)
        }

    // endregion

    // region updateBatteryEvent

    @Test
    fun `updateBatteryEvent updates date and notes`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(id = "dev-1", name = "Alarm")
            val event = TestDevices.createBatteryEvent(
                id = "evt-1",
                deviceId = "dev-1",
                date = Instant.parse("2024-01-01T00:00:00Z"),
            )
            repo.setDevices(listOf(device))
            repo.setEvents(listOf(event))
            val (handler, _) = createHandler(repo)

            val result = handler.execute(
                AiToolNames.UPDATE_BATTERY_EVENT,
                mapOf(
                    AiToolParams.ID to "evt-1",
                    AiToolParams.NEW_DATE to "2024-06-15",
                    AiToolParams.NOTES to "Replaced with Duracell",
                ),
            )

            assertTrue(result.startsWith("Success:"))
            assertTrue(result.contains("2024-06-15"))
            assertTrue(result.contains("Replaced with Duracell"))
            assertEquals("Replaced with Duracell", repo.events[0].notes)
        }

    @Test
    fun `updateBatteryEvent recalculates device lastReplaced on date change`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(
                id = "dev-1",
                name = "Alarm",
                batteryLastReplaced = Instant.parse("2024-01-01T00:00:00Z"),
            )
            val event = TestDevices.createBatteryEvent(
                id = "evt-1",
                deviceId = "dev-1",
                date = Instant.parse("2024-01-01T00:00:00Z"),
            )
            repo.setDevices(listOf(device))
            repo.setEvents(listOf(event))
            val (handler, _) = createHandler(repo)

            handler.execute(
                AiToolNames.UPDATE_BATTERY_EVENT,
                mapOf(AiToolParams.ID to "evt-1", AiToolParams.NEW_DATE to "2024-06-15"),
            )

            // Device lastReplaced should be recalculated to the new event date
            assertTrue(repo.devices[0].batteryLastReplaced > Instant.parse("2024-01-01T00:00:00Z"))
        }

    @Test
    fun `updateBatteryEvent missing id returns error`() =
        runTest {
            val (handler, _) = createHandler()

            val result = handler.execute(AiToolNames.UPDATE_BATTERY_EVENT, emptyMap())

            assertEquals("Error: Missing id", result)
        }

    @Test
    fun `updateBatteryEvent nonexistent id returns error`() =
        runTest {
            val (handler, _) = createHandler()

            val result = handler.execute(
                AiToolNames.UPDATE_BATTERY_EVENT,
                mapOf(AiToolParams.ID to "nonexistent"),
            )

            assertEquals("Error: Battery event not found with id 'nonexistent'", result)
        }

    // endregion

    // region deleteBatteryEvent

    @Test
    fun `deleteBatteryEvent removes event and recalculates lastReplaced`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(
                id = "dev-1",
                name = "Alarm",
                batteryLastReplaced = Instant.parse("2024-06-15T00:00:00Z"),
            )
            val event1 = TestDevices.createBatteryEvent(
                id = "evt-1",
                deviceId = "dev-1",
                date = Instant.parse("2024-01-01T00:00:00Z"),
            )
            val event2 = TestDevices.createBatteryEvent(
                id = "evt-2",
                deviceId = "dev-1",
                date = Instant.parse("2024-06-15T00:00:00Z"),
            )
            repo.setDevices(listOf(device))
            repo.setEvents(listOf(event1, event2))
            val (handler, _) = createHandler(repo)

            val result = handler.execute(
                AiToolNames.DELETE_BATTERY_EVENT,
                mapOf(AiToolParams.ID to "evt-2"),
            )

            assertTrue(result.startsWith("Success:"))
            assertTrue(result.contains("Alarm"))
            assertEquals(1, repo.events.size)
            assertEquals("evt-1", repo.events[0].id)
            // lastReplaced should be recalculated to the remaining event's date
            assertEquals(Instant.parse("2024-01-01T00:00:00Z"), repo.devices[0].batteryLastReplaced)
        }

    @Test
    fun `deleteBatteryEvent resets lastReplaced when no events remain`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(
                id = "dev-1",
                name = "Alarm",
                batteryLastReplaced = Instant.parse("2024-06-15T00:00:00Z"),
            )
            val event = TestDevices.createBatteryEvent(
                id = "evt-1",
                deviceId = "dev-1",
                date = Instant.parse("2024-06-15T00:00:00Z"),
            )
            repo.setDevices(listOf(device))
            repo.setEvents(listOf(event))
            val (handler, _) = createHandler(repo)

            handler.execute(
                AiToolNames.DELETE_BATTERY_EVENT,
                mapOf(AiToolParams.ID to "evt-1"),
            )

            assertTrue(repo.events.isEmpty())
            assertEquals(Instant.fromEpochMilliseconds(0), repo.devices[0].batteryLastReplaced)
        }

    @Test
    fun `deleteBatteryEvent missing id returns error`() =
        runTest {
            val (handler, _) = createHandler()

            val result = handler.execute(AiToolNames.DELETE_BATTERY_EVENT, emptyMap())

            assertEquals("Error: Missing id", result)
        }

    @Test
    fun `deleteBatteryEvent nonexistent id returns error`() =
        runTest {
            val (handler, _) = createHandler()

            val result = handler.execute(
                AiToolNames.DELETE_BATTERY_EVENT,
                mapOf(AiToolParams.ID to "nonexistent"),
            )

            assertEquals("Error: Battery event not found with id 'nonexistent'", result)
        }

    // endregion

    // region General

    @Test
    fun `execute with unknown tool name returns error`() =
        runTest {
            val (handler, _) = createHandler()

            val result = handler.execute("unknownTool", emptyMap())

            assertEquals("Error: Unknown tool 'unknownTool'", result)
        }

    // endregion
}
