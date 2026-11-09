package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.ai.AiChangeKind
import com.chriscartland.batterybutler.domain.model.ai.AiToolNames
import com.chriscartland.batterybutler.domain.model.ai.AiToolParams
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.FakeNeedsBatteryRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class DeviceToolHandlerTest {
    private class Harness(
        val repo: FakeDeviceRepository = FakeDeviceRepository(),
        val marks: FakeNeedsBatteryRepository = FakeNeedsBatteryRepository(),
    ) {
        val handler = DeviceToolHandler(repo, marks, UpdateDeviceLastReplacedUseCase(repo))

        suspend fun marked(id: String) = id in marks.getNeedsBatteryMarks().first()
    }

    private fun day(iso: String): Instant = Instant.fromEpochMilliseconds(LocalDate.parse(iso).atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds())

    private suspend fun ToolOutcome.undoNow() {
        assertIs<Result.Success<Unit>>(assertNotNull(undo, "an undo was expected")())
    }

    // ─── devices ────────────────────────────────────────────────────────────────

    @Test
    fun `addDevice stores the location and reports what it stored`() =
        runTest {
            val h = Harness()
            val outcome = h.handler.run(
                AiToolNames.ADD_DEVICE,
                mapOf(AiToolParams.NAME to "Hall Alarm", AiToolParams.TYPE to "Smoke Alarm", AiToolParams.LOCATION to "Hall"),
            )
            assertTrue(outcome.modelResult.startsWith("Success:"), outcome.modelResult)
            val device = h.repo.devices.single()
            assertEquals("Hall", device.location)
            assertEquals(
                "Smoke Alarm",
                h.repo.deviceTypes
                    .single()
                    .name,
            )
        }

    @Test
    fun `addDevice with no type uses the catch-all type like the add screen`() =
        runTest {
            val h = Harness()
            h.handler.run(AiToolNames.ADD_DEVICE, mapOf(AiToolParams.NAME to "Remote"))
            assertEquals(
                "default_type",
                h.repo.devices
                    .single()
                    .typeId,
            )
            assertTrue(h.repo.deviceTypes.isEmpty())
        }

    @Test
    fun `addDevice reuses an existing type regardless of case`() =
        runTest {
            val h = Harness()
            h.repo.setDeviceTypes(listOf(TestDevices.createDeviceType(id = "t1", name = "Thermostat")))
            h.handler.run(AiToolNames.ADD_DEVICE, mapOf(AiToolParams.NAME to "Upstairs", AiToolParams.TYPE to "thermostat"))
            assertEquals(
                "t1",
                h.repo.devices
                    .single()
                    .typeId,
            )
            assertEquals(1, h.repo.deviceTypes.size)
        }

    @Test
    fun `addDevice refuses a duplicate name and names the existing id`() =
        runTest {
            val h = Harness()
            h.repo.setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Hall Alarm")))
            val outcome = h.handler.run(AiToolNames.ADD_DEVICE, mapOf(AiToolParams.NAME to "hall alarm"))
            assertTrue(outcome.modelResult.startsWith("Error:"))
            assertTrue(outcome.modelResult.contains("id:d1"))
            assertNull(outcome.undo)
            assertEquals(1, h.repo.devices.size)
        }

    @Test
    fun `undoing addDevice removes the device and the type it created`() =
        runTest {
            val h = Harness()
            val outcome = h.handler.run(AiToolNames.ADD_DEVICE, mapOf(AiToolParams.NAME to "Hall Alarm", AiToolParams.TYPE to "Smoke Alarm"))
            outcome.undoNow()
            assertTrue(h.repo.devices.isEmpty())
            assertTrue(h.repo.deviceTypes.isEmpty())
        }

    @Test
    fun `undoing addDevice keeps a created type that another device now uses`() =
        runTest {
            val h = Harness()
            val first = h.handler.run(AiToolNames.ADD_DEVICE, mapOf(AiToolParams.NAME to "A", AiToolParams.TYPE to "Lock"))
            h.handler.run(AiToolNames.ADD_DEVICE, mapOf(AiToolParams.NAME to "B", AiToolParams.TYPE to "Lock"))
            first.undoNow()
            assertEquals(listOf("B"), h.repo.devices.map { it.name })
            assertEquals(1, h.repo.deviceTypes.size)
        }

    @Test
    fun `updateDevice changes only the fields given and can be undone`() =
        runTest {
            val h = Harness()
            h.repo.setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Hall Alarm", location = "Hall", batteryLastReplaced = day("2026-01-01"))))
            val outcome = h.handler.run(AiToolNames.UPDATE_DEVICE, mapOf(AiToolParams.ID to "d1", AiToolParams.NEW_LOCATION to "Landing"))
            assertEquals(
                "Landing",
                h.repo.devices
                    .single()
                    .location,
            )
            assertEquals(
                day("2026-01-01"),
                h.repo.devices
                    .single()
                    .batteryLastReplaced,
            )

            outcome.undoNow()
            assertEquals(
                "Hall",
                h.repo.devices
                    .single()
                    .location,
            )
        }

    @Test
    fun `updateDevice with nothing new is a success with no action`() =
        runTest {
            val h = Harness()
            h.repo.setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Hall Alarm")))
            val outcome = h.handler.run(AiToolNames.UPDATE_DEVICE, mapOf(AiToolParams.ID to "d1", AiToolParams.NEW_NAME to "Hall Alarm"))
            assertTrue(outcome.modelResult.startsWith("Success:"))
            assertNull(outcome.undo)
        }

    @Test
    fun `deleteDevice removes its events and mark and and undo brings all three back`() =
        runTest {
            val h = Harness(marks = FakeNeedsBatteryRepository(initialFlagged = setOf("d1")))
            h.repo.setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Hall Alarm")))
            h.repo.setEvents(listOf(TestDevices.createBatteryEvent(id = "e1", deviceId = "d1"), TestDevices.createBatteryEvent(id = "e2", deviceId = "d1")))

            val outcome = h.handler.run(AiToolNames.DELETE_DEVICE, mapOf(AiToolParams.ID to "d1"))
            assertTrue(h.repo.devices.isEmpty())
            assertTrue(h.repo.events.isEmpty())
            assertFalse(h.marked("d1"))

            outcome.undoNow()
            assertEquals(
                "Hall Alarm",
                h.repo.devices
                    .single()
                    .name,
            )
            assertEquals(
                setOf("e1", "e2"),
                h.repo.events
                    .map { it.id }
                    .toSet(),
            )
            assertTrue(h.marked("d1"))
        }

    private suspend fun Harness.ready(
        name: String,
        args: Map<String, Any?>,
    ): ToolPreview.Ready = assertIs<ToolPreview.Ready>(handler.preview(name, args))

    @Test
    fun `a preview writes nothing`() =
        runTest {
            val h = Harness()
            h.repo.setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Hall Alarm")))
            h.handler.preview(AiToolNames.ADD_DEVICE, mapOf(AiToolParams.NAME to "New", AiToolParams.TYPE to "Fresh Type"))
            h.handler.preview(AiToolNames.RECORD_BATTERY_REPLACEMENT, mapOf(AiToolParams.DEVICE_NAME to "Hall Alarm"))
            h.handler.preview(AiToolNames.DELETE_DEVICE, mapOf(AiToolParams.ID to "d1"))
            assertEquals(1, h.repo.devices.size)
            assertTrue(h.repo.events.isEmpty())
            assertTrue(h.repo.deviceTypes.isEmpty())
        }

    @Test
    fun `a delete preview says what goes with it and warns about the photo`() =
        runTest {
            val h = Harness()
            h.repo.setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Hall Alarm").copy(imageEtag = "abc")))
            h.repo.setEvents(listOf(TestDevices.createBatteryEvent(id = "e1", deviceId = "d1")))
            val p = h.ready(AiToolNames.DELETE_DEVICE, mapOf(AiToolParams.ID to "d1"))
            assertEquals(AiChangeKind.DELETE, p.kind)
            assertEquals("Delete Hall Alarm and its 1 battery change", p.summary)
            assertEquals("Undo can't bring back its photo.", p.warning)
        }

    @Test
    fun `an update preview shows old and new values`() =
        runTest {
            val h = Harness()
            h.repo.setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Hall Alarm", location = "Hall")))
            val p = h.ready(AiToolNames.UPDATE_DEVICE, mapOf(AiToolParams.ID to "d1", AiToolParams.NEW_LOCATION to "Landing"))
            assertEquals("Update Hall Alarm: location Hall → Landing", p.summary)
        }

    @Test
    fun `recording for an unknown device warns that it adds one`() =
        runTest {
            val h = Harness()
            val p = h.ready(AiToolNames.RECORD_BATTERY_REPLACEMENT, mapOf(AiToolParams.DEVICE_NAME to "Hal Alarm", AiToolParams.DATE to "2026-03-09", AiToolParams.NOTES to "fresh"))
            assertEquals("Record a battery change for Hal Alarm on 2026-03-09 — fresh", p.summary)
            assertEquals("There's no device called Hal Alarm yet, so this also adds one.", p.warning)
        }

    @Test
    fun `previews refuse what could never work`() =
        runTest {
            val h = Harness()
            h.repo.setDeviceTypes(listOf(TestDevices.createDeviceType(id = "t1", name = "Lock")))
            h.repo.setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Hall Alarm", typeId = "t1")))
            listOf(
                AiToolNames.ADD_DEVICE to mapOf(AiToolParams.NAME to "hall alarm"),
                AiToolNames.ADD_DEVICE_TYPE to mapOf(AiToolParams.NAME to "lock"),
                AiToolNames.DELETE_DEVICE_TYPE to mapOf(AiToolParams.ID to "t1"),
                AiToolNames.DELETE_DEVICE to mapOf(AiToolParams.ID to "ghost"),
                AiToolNames.RECORD_BATTERY_REPLACEMENT to mapOf(AiToolParams.DEVICE_NAME to "Hall Alarm", AiToolParams.DATE to "yesterday"),
                AiToolNames.RECORD_BATTERY_REPLACEMENT to mapOf(AiToolParams.DEVICE_NAME to "Hall Alarm", AiToolParams.DATE to "2999-01-01"),
                "fly" to emptyMap(),
            ).forEach { (name, args) -> assertIs<ToolPreview.Refused>(h.handler.preview(name, args), "$name $args") }
        }

    @Test
    fun `undo won't overwrite a field edited again since`() =
        runTest {
            val h = Harness()
            h.repo.setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Hall Alarm", location = "Hall")))
            val move = h.handler.run(AiToolNames.UPDATE_DEVICE, mapOf(AiToolParams.ID to "d1", AiToolParams.NEW_LOCATION to "Landing"))
            h.handler.run(AiToolNames.UPDATE_DEVICE, mapOf(AiToolParams.ID to "d1", AiToolParams.NEW_LOCATION to "Attic"))
            val result = assertIs<Result.Error<*>>(assertNotNull(move.undo)())
            assertTrue(result.error.toString().contains("changed again since"), result.error.toString())
            assertEquals(
                "Attic",
                h.repo.devices
                    .single()
                    .location,
            )
        }

    @Test
    fun `undoing an update leaves later edits to other fields alone`() =
        runTest {
            val h = Harness()
            h.repo.setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Hall Alarm", location = "Hall")))
            val rename = h.handler.run(AiToolNames.UPDATE_DEVICE, mapOf(AiToolParams.ID to "d1", AiToolParams.NEW_NAME to "Landing Alarm"))
            h.handler.run(AiToolNames.UPDATE_DEVICE, mapOf(AiToolParams.ID to "d1", AiToolParams.NEW_LOCATION to "Landing"))
            rename.undoNow()
            assertEquals(
                "Hall Alarm",
                h.repo.devices
                    .single()
                    .name,
            )
            assertEquals(
                "Landing",
                h.repo.devices
                    .single()
                    .location,
            )
        }

    @Test
    fun `undoing an update to something deleted since says so`() =
        runTest {
            val h = Harness()
            h.repo.setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Hall Alarm")))
            val rename = h.handler.run(AiToolNames.UPDATE_DEVICE, mapOf(AiToolParams.ID to "d1", AiToolParams.NEW_NAME to "X"))
            h.repo.setDevices(emptyList())
            assertIs<Result.Error<*>>(assertNotNull(rename.undo)())
            assertTrue(h.repo.devices.isEmpty(), "an undo must not resurrect it")
        }

    @Test
    fun `undoing an older recorded change keeps the newer one's date`() =
        runTest {
            val h = Harness()
            h.repo.setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Hall Alarm", batteryLastReplaced = day("2026-01-01"))))
            val older = h.handler.run(AiToolNames.RECORD_BATTERY_REPLACEMENT, mapOf(AiToolParams.DEVICE_NAME to "Hall Alarm", AiToolParams.DATE to "2026-03-01"))
            h.handler.run(AiToolNames.RECORD_BATTERY_REPLACEMENT, mapOf(AiToolParams.DEVICE_NAME to "Hall Alarm", AiToolParams.DATE to "2026-05-01"))
            older.undoNow()
            assertEquals(
                day("2026-05-01"),
                h.repo.devices
                    .single()
                    .batteryLastReplaced,
            )
        }

    @Test
    fun `undoing the only recorded change restores a date that never came from an event`() =
        runTest {
            val h = Harness()
            h.repo.setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Hall Alarm", batteryLastReplaced = day("2026-01-01"))))
            val change = h.handler.run(AiToolNames.RECORD_BATTERY_REPLACEMENT, mapOf(AiToolParams.DEVICE_NAME to "Hall Alarm", AiToolParams.DATE to "2026-03-01"))
            change.undoNow()
            assertEquals(
                day("2026-01-01"),
                h.repo.devices
                    .single()
                    .batteryLastReplaced,
            )
        }

    @Test
    fun `adding a device can't be undone once it has battery changes`() =
        runTest {
            val h = Harness()
            val add = h.handler.run(AiToolNames.ADD_DEVICE, mapOf(AiToolParams.NAME to "Hall Alarm"))
            h.handler.run(AiToolNames.RECORD_BATTERY_REPLACEMENT, mapOf(AiToolParams.DEVICE_NAME to "Hall Alarm"))
            assertIs<Result.Error<*>>(assertNotNull(add.undo)())
            assertEquals(1, h.repo.devices.size)
            assertEquals(1, h.repo.events.size)
        }

    // ─── types ──────────────────────────────────────────────────────────────────

    @Test
    fun `addDeviceType stores the battery and quantity it was given`() =
        runTest {
            val h = Harness()
            val outcome = h.handler.run(
                AiToolNames.ADD_DEVICE_TYPE,
                mapOf(AiToolParams.NAME to "Smoke Alarm", AiToolParams.BATTERY_TYPE to "9V", AiToolParams.BATTERY_QUANTITY to "1", AiToolParams.ICON to "detector_smoke"),
            )
            val type = h.repo.deviceTypes.single()
            assertEquals("9V", type.batteryType)
            assertEquals(1, type.batteryQuantity)
            assertEquals("detector_smoke", type.defaultIcon)
        }

    @Test
    fun `addDeviceType falls back to the default icon for one it doesn't have`() =
        runTest {
            val h = Harness()
            h.handler.run(AiToolNames.ADD_DEVICE_TYPE, mapOf(AiToolParams.NAME to "Toy", AiToolParams.BATTERY_TYPE to "AA", AiToolParams.BATTERY_QUANTITY to 2, AiToolParams.ICON to "unicorn"))
            assertEquals(
                "default",
                h.repo.deviceTypes
                    .single()
                    .defaultIcon,
            )
            assertEquals(
                2,
                h.repo.deviceTypes
                    .single()
                    .batteryQuantity,
            )
        }

    @Test
    fun `deleteDeviceType is refused while devices use it`() =
        runTest {
            val h = Harness()
            h.repo.setDeviceTypes(listOf(TestDevices.createDeviceType(id = "t1", name = "Lock")))
            h.repo.setDevices(listOf(TestDevices.createDevice(id = "d1", typeId = "t1")))
            val outcome = h.handler.run(AiToolNames.DELETE_DEVICE_TYPE, mapOf(AiToolParams.ID to "t1"))
            assertTrue(outcome.modelResult.startsWith("Error:"))
            assertEquals(1, h.repo.deviceTypes.size)
        }

    @Test
    fun `updateDeviceType and deleteDeviceType can be undone`() =
        runTest {
            val h = Harness()
            h.repo.setDeviceTypes(listOf(TestDevices.createDeviceType(id = "t1", name = "Lock", batteryType = "AA", batteryQuantity = 4)))
            val update = h.handler.run(AiToolNames.UPDATE_DEVICE_TYPE, mapOf(AiToolParams.ID to "t1", AiToolParams.NEW_BATTERY_QUANTITY to 8))
            assertEquals(
                8,
                h.repo.deviceTypes
                    .single()
                    .batteryQuantity,
            )
            update.undoNow()
            assertEquals(
                4,
                h.repo.deviceTypes
                    .single()
                    .batteryQuantity,
            )

            val delete = h.handler.run(AiToolNames.DELETE_DEVICE_TYPE, mapOf(AiToolParams.ID to "t1"))
            assertTrue(h.repo.deviceTypes.isEmpty())
            delete.undoNow()
            assertEquals(
                "Lock",
                h.repo.deviceTypes
                    .single()
                    .name,
            )
        }

    // ─── battery changes ────────────────────────────────────────────────────────

    @Test
    fun `recordBatteryReplacement uses the type's battery and the note and and today when no date is given`() =
        runTest {
            val h = Harness(marks = FakeNeedsBatteryRepository(initialFlagged = setOf("d1")))
            h.repo.setDeviceTypes(listOf(TestDevices.createDeviceType(id = "t1", batteryType = "9V")))
            h.repo.setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Hall Alarm", typeId = "t1")))

            val outcome = h.handler.run(
                AiToolNames.RECORD_BATTERY_REPLACEMENT,
                mapOf(AiToolParams.DEVICE_NAME to "hall alarm", AiToolParams.NOTES to "Fresh pack"),
            )
            val event = h.repo.events.single()
            assertEquals("9V", event.batteryType)
            assertEquals("Fresh pack", event.notes)
            assertTrue(event.date > day("2020-01-01"))
            assertEquals(
                event.date,
                h.repo.devices
                    .single()
                    .batteryLastReplaced,
            )
            assertFalse(h.marked("d1"), "recording a change clears the mark")
        }

    @Test
    fun `recordBatteryReplacement creates a missing device with its type and location`() =
        runTest {
            val h = Harness()
            val outcome = h.handler.run(
                AiToolNames.RECORD_BATTERY_REPLACEMENT,
                mapOf(AiToolParams.DEVICE_NAME to "Garage Sensor", AiToolParams.DATE to "2026-03-09", AiToolParams.DEVICE_TYPE to "Sensor", AiToolParams.LOCATION to "Garage"),
            )
            val device = h.repo.devices.single()
            assertEquals("Garage", device.location)
            assertEquals(
                "Sensor",
                h.repo.deviceTypes
                    .single()
                    .name,
            )
            assertEquals(day("2026-03-09"), device.batteryLastReplaced)

            outcome.undoNow()
            assertTrue(h.repo.devices.isEmpty())
            assertTrue(h.repo.events.isEmpty())
            assertTrue(h.repo.deviceTypes.isEmpty())
        }

    @Test
    fun `a backdated change doesn't move the device's date and and undo restores the mark`() =
        runTest {
            val h = Harness(marks = FakeNeedsBatteryRepository(initialFlagged = setOf("d1")))
            h.repo.setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Hall Alarm", batteryLastReplaced = day("2026-06-01"))))
            val outcome = h.handler.run(AiToolNames.RECORD_BATTERY_REPLACEMENT, mapOf(AiToolParams.DEVICE_NAME to "Hall Alarm", AiToolParams.DATE to "2025-01-01"))
            assertEquals(
                day("2026-06-01"),
                h.repo.devices
                    .single()
                    .batteryLastReplaced,
            )
            outcome.undoNow()
            assertTrue(h.repo.events.isEmpty())
            assertTrue(h.marked("d1"))
        }

    @Test
    fun `a bad date is refused before anything is written`() =
        runTest {
            val h = Harness()
            h.repo.setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Hall Alarm")))
            val outcome = h.handler.run(AiToolNames.RECORD_BATTERY_REPLACEMENT, mapOf(AiToolParams.DEVICE_NAME to "Hall Alarm", AiToolParams.DATE to "yesterday"))
            assertTrue(outcome.modelResult.startsWith("Error:"))
            assertTrue(h.repo.events.isEmpty())
        }

    @Test
    fun `editing or deleting a change recomputes the device's date and and undo recomputes again`() =
        runTest {
            val h = Harness()
            h.repo.setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Hall Alarm", batteryLastReplaced = day("2026-05-01"))))
            h.repo.setEvents(
                listOf(
                    TestDevices.createBatteryEvent(id = "old", deviceId = "d1", date = day("2026-01-01")),
                    TestDevices.createBatteryEvent(id = "new", deviceId = "d1", date = day("2026-05-01")),
                ),
            )
            val edit = h.handler.run(AiToolNames.UPDATE_BATTERY_EVENT, mapOf(AiToolParams.ID to "new", AiToolParams.NEW_DATE to "2025-12-01"))
            assertEquals(
                day("2026-01-01"),
                h.repo.devices
                    .single()
                    .batteryLastReplaced,
            )
            edit.undoNow()
            assertEquals(
                day("2026-05-01"),
                h.repo.devices
                    .single()
                    .batteryLastReplaced,
            )

            val delete = h.handler.run(AiToolNames.DELETE_BATTERY_EVENT, mapOf(AiToolParams.ID to "new"))
            assertEquals(
                day("2026-01-01"),
                h.repo.devices
                    .single()
                    .batteryLastReplaced,
            )
            delete.undoNow()
            assertEquals(2, h.repo.events.size)
            assertEquals(
                day("2026-05-01"),
                h.repo.devices
                    .single()
                    .batteryLastReplaced,
            )
        }

    // ─── marks and reads ────────────────────────────────────────────────────────

    @Test
    fun `markNeedsBattery sets and clears the mark and and undo puts it back`() =
        runTest {
            val h = Harness()
            h.repo.setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Hall Alarm")))
            val mark = h.handler.run(AiToolNames.MARK_NEEDS_BATTERY, mapOf(AiToolParams.ID to "d1"))
            assertTrue(h.marked("d1"))

            val again = h.handler.run(AiToolNames.MARK_NEEDS_BATTERY, mapOf(AiToolParams.ID to "d1", AiToolParams.NEEDS_BATTERY to "true"))
            assertNull(again.undo)

            val clear = h.handler.run(AiToolNames.MARK_NEEDS_BATTERY, mapOf(AiToolParams.ID to "d1", AiToolParams.NEEDS_BATTERY to "false"))
            assertFalse(h.marked("d1"))
            clear.undoNow()
            assertTrue(h.marked("d1"))
        }

    @Test
    fun `an unknown tool and a missing id are errors and not exceptions`() =
        runTest {
            val h = Harness()
            assertTrue(
                h.handler
                    .run("fly", emptyMap())
                    .modelResult
                    .startsWith("Error: Unknown tool"),
            )
            assertTrue(
                h.handler
                    .run(AiToolNames.UPDATE_DEVICE, emptyMap())
                    .modelResult
                    .startsWith("Error: Missing id"),
            )
        }
}
