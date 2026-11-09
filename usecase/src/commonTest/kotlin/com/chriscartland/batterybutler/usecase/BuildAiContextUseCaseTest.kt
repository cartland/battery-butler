package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.FakeNeedsBatteryRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class BuildAiContextUseCaseTest {
    private val now = Instant.fromEpochMilliseconds(1_780_000_000_000)

    @Test
    fun `each device line carries what the model needs to answer and to act`() =
        runTest {
            val repo = FakeDeviceRepository()
            repo.setDeviceTypes(listOf(TestDevices.createDeviceType(id = "t1", name = "Smoke Alarm", batteryType = "9V", batteryQuantity = 1)))
            repo.setDevices(
                listOf(
                    TestDevices.createDevice(id = "d1", name = "Hall Alarm", typeId = "t1", location = "Hall", batteryLastReplaced = now - 200.days),
                    TestDevices.createDevice(id = "d2", name = "Attic Alarm", typeId = "t1", batteryLastReplaced = now - 400.days),
                    TestDevices.createDevice(id = "d3", name = "New One", typeId = "t1"),
                ),
            )
            repo.setEvents(listOf(TestDevices.createBatteryEvent(id = "e1", deviceId = "d1", date = now - 200.days, notes = "fresh")))
            val marks = FakeNeedsBatteryRepository(initialFlagged = setOf("d2"))

            val text = BuildAiContextUseCase(repo, marks)(now)

            assertTrue(text.contains("Today is"), text)
            assertTrue(text.contains("id:d1 | Hall Alarm | Smoke Alarm | 1×9V | Hall | "), text)
            assertTrue(text.contains("(200 days ago) | due | 1"), text)
            assertTrue(text.contains("| overdue, marked as needing a battery | 0"), text)
            assertTrue(text.contains("| never | unknown | 0"), text)
            assertTrue(text.contains("id:t1 | Smoke Alarm | 1×9V | default | 3"), text)
            assertTrue(text.contains("id:e1 | Hall Alarm | "), text)
            assertTrue(text.contains("| AA | fresh"), text)
        }

    @Test
    fun `an empty inventory still reads as a situation`() =
        runTest {
            val text = BuildAiContextUseCase(FakeDeviceRepository(), FakeNeedsBatteryRepository())(now)
            assertTrue(text.contains("Devices (0)"), text)
            assertTrue(text.contains("Types (0)"), text)
            assertTrue(text.contains("0 of 0"), text)
        }

    @Test
    fun `recent changes are capped and the model is pointed at the history tool`() =
        runTest {
            val repo = FakeDeviceRepository()
            repo.setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Hall Alarm")))
            repo.setEvents((1..40).map { TestDevices.createBatteryEvent(id = "e$it", deviceId = "d1", date = now - it.days) })
            val text = BuildAiContextUseCase(repo, FakeNeedsBatteryRepository())(now)
            assertTrue(text.contains("30 of 40 — call getDeviceHistory"), text)
            assertTrue(text.contains("id:e1 |"), text)
            assertTrue(!text.contains("id:e31 |"))
        }

    @Test
    fun `a huge inventory stays inside the relay's limit by showing fewer changes`() =
        runTest {
            val repo = FakeDeviceRepository()
            repo.setDevices((1..120).map { TestDevices.createDevice(id = "device-$it", name = "Device number $it with a longish name", location = "Somewhere on floor $it") })
            repo.setEvents((1..40).map { TestDevices.createBatteryEvent(id = "e$it", deviceId = "device-1", date = now - it.days, notes = "note ".repeat(20)) })
            val text = BuildAiContextUseCase(repo, FakeNeedsBatteryRepository())(now)
            assertTrue(text.length < 20_000, "${text.length} chars")
            assertTrue(text.contains("8 of 40"), text)
        }

    @Test
    fun `an inventory too big for the relay is cut off and says so`() =
        runTest {
            val repo = FakeDeviceRepository()
            repo.setDevices((1..400).map { TestDevices.createDevice(id = "device-$it", name = "Device number $it with a longish name", location = "Somewhere on floor $it") })
            val text = BuildAiContextUseCase(repo, FakeNeedsBatteryRepository())(now)
            assertTrue(text.length < 17_000, "${text.length} chars")
            assertTrue(text.endsWith("Say so if it matters.]"), text.takeLast(120))
        }
}
