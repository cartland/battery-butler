package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.ai.AiToolNames
import com.chriscartland.batterybutler.domain.model.ai.AiToolParams
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.FakeNeedsBatteryRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class DeviceReadToolsTest {
    private val now = Instant.fromEpochMilliseconds(1_780_000_000_000)

    private fun tools(): Pair<DeviceReadTools, FakeDeviceRepository> {
        val repo = FakeDeviceRepository()
        repo.setDeviceTypes(
            listOf(
                TestDevices.createDeviceType(id = "t1", name = "Smoke Alarm", batteryType = "9V", batteryQuantity = 1),
                TestDevices.createDeviceType(id = "t2", name = "Lock", batteryType = "AA", batteryQuantity = 4),
            ),
        )
        repo.setDevices(
            listOf(
                TestDevices.createDevice(id = "due", name = "Hall Alarm", typeId = "t1", location = "Hall", batteryLastReplaced = now - 200.days),
                TestDevices.createDevice(id = "late", name = "Front Door", typeId = "t2", batteryLastReplaced = now - 400.days),
                TestDevices.createDevice(id = "fresh", name = "Back Door", typeId = "t2", batteryLastReplaced = now - 3.days),
                TestDevices.createDevice(id = "never", name = "Gate", typeId = "t2", batteryLastReplaced = Instant.fromEpochMilliseconds(0)),
            ),
        )
        repo.setEvents(
            listOf(
                TestDevices.createBatteryEvent(id = "e1", deviceId = "due", date = now - 200.days, notes = "first"),
                TestDevices.createBatteryEvent(id = "e2", deviceId = "fresh", date = now - 3.days),
            ),
        )
        return DeviceReadTools(repo, FakeNeedsBatteryRepository(initialFlagged = setOf("fresh"))) to repo
    }

    @Test
    fun `listDevices filters by status and search`() =
        runTest {
            val (t, _) = tools()
            val overdue = t.run(AiToolNames.LIST_DEVICES, mapOf(AiToolParams.STATUS to "overdue"), now).modelResult
            assertTrue(overdue.startsWith("1 device(s)"), overdue)
            assertTrue(overdue.contains("Front Door"))
            assertTrue(t.run(AiToolNames.LIST_DEVICES, mapOf(AiToolParams.STATUS to "marked"), now).modelResult.contains("Back Door"))
            val search = t.run(AiToolNames.LIST_DEVICES, mapOf(AiToolParams.SEARCH to "hall"), now).modelResult
            assertTrue(search.contains("Hall Alarm") && !search.contains("Front Door"), search)
        }

    @Test
    fun `getDevice reports everything about one device`() =
        runTest {
            val (t, _) = tools()
            val text = t.run(AiToolNames.GET_DEVICE, mapOf(AiToolParams.ID to "due"), now).modelResult
            assertTrue(text.contains("batteries: 1×9V") && text.contains("status: due") && text.contains("changes recorded: 1"), text)
        }

    @Test
    fun `an unknown id is an error with no lookup line`() =
        runTest {
            val (t, _) = tools()
            val r = t.run(AiToolNames.GET_DEVICE, mapOf(AiToolParams.ID to "nope"), now)
            assertTrue(r.modelResult.startsWith("Error:"))
            assertEquals(null, r.lookup)
        }

    @Test
    fun `listBatteryChanges honours its range and limit`() =
        runTest {
            val (t, _) = tools()
            val all = t.run(AiToolNames.LIST_BATTERY_CHANGES, emptyMap(), now).modelResult
            assertTrue(all.indexOf("id:e2") < all.indexOf("id:e1"), all)
            val one = t.run(AiToolNames.LIST_BATTERY_CHANGES, mapOf(AiToolParams.LIMIT to "1"), now).modelResult
            assertTrue(one.startsWith("1 of 2"), one)
            assertTrue(t.run(AiToolNames.LIST_BATTERY_CHANGES, mapOf(AiToolParams.FROM to "soon"), now).modelResult.startsWith("Error:"))
        }

    @Test
    fun `getBatteriesToBuy adds up due overdue and marked devices`() =
        runTest {
            val (t, _) = tools()
            val text = t.run(AiToolNames.GET_BATTERIES_TO_BUY, emptyMap(), now).modelResult
            assertTrue(text.contains("3 device(s)") && text.contains("8 × AA") && text.contains("1 × 9V"), text)
        }

    @Test
    fun `reading changes nothing`() =
        runTest {
            val (t, repo) = tools()
            val before = repo.devices.toList() to repo.events.toList()
            AiToolNames.READ_ONLY.forEach { t.run(it, mapOf(AiToolParams.ID to "due"), now) }
            assertEquals(before, repo.devices.toList() to repo.events.toList())
        }

    @Test
    fun `a write tool is not a read`() =
        runTest {
            val (t, repo) = tools()
            assertFalse(AiToolNames.DELETE_DEVICE in AiToolNames.READ_ONLY)
            assertTrue(t.run(AiToolNames.DELETE_DEVICE, mapOf(AiToolParams.ID to "due"), now).modelResult.startsWith("Error: Unknown tool"))
            assertEquals(4, repo.devices.size)
        }
}
