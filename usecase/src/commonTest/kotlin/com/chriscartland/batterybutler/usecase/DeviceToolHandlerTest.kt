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
        val handler = DeviceToolHandler(repo, findOrCreateType, findOrCreateDevice)
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
            val findOrCreateType = FindOrCreateDeviceTypeUseCase(repo)
            val findOrCreateDevice = FindOrCreateDeviceUseCase(repo, findOrCreateType)
            val handler = DeviceToolHandler(repo, findOrCreateType, findOrCreateDevice)

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
            val findOrCreateType = FindOrCreateDeviceTypeUseCase(repo)
            val findOrCreateDevice = FindOrCreateDeviceUseCase(repo, findOrCreateType)
            val handler = DeviceToolHandler(repo, findOrCreateType, findOrCreateDevice)

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
            val findOrCreateType = FindOrCreateDeviceTypeUseCase(repo)
            val findOrCreateDevice = FindOrCreateDeviceUseCase(repo, findOrCreateType)
            val handler = DeviceToolHandler(repo, findOrCreateType, findOrCreateDevice)

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
            val findOrCreateType = FindOrCreateDeviceTypeUseCase(repo)
            val findOrCreateDevice = FindOrCreateDeviceUseCase(repo, findOrCreateType)
            val handler = DeviceToolHandler(repo, findOrCreateType, findOrCreateDevice)

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
