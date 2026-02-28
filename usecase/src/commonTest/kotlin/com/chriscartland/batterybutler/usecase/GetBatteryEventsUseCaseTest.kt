package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class GetBatteryEventsUseCaseTest {
    @Test
    fun `invoke returns all events`() =
        runTest {
            val fakeRepository = FakeDeviceRepository()
            val event1 = TestDevices.createBatteryEvent(id = "e1", deviceId = "d1")
            val event2 = TestDevices.createBatteryEvent(id = "e2", deviceId = "d2")
            fakeRepository.setEvents(listOf(event1, event2))
            val useCase = GetBatteryEventsUseCase(fakeRepository)

            val result = useCase().first()

            assertEquals(2, result.size)
            assertTrue(result.contains(event1))
            assertTrue(result.contains(event2))
        }

    @Test
    fun `forDevice filters by device ID`() =
        runTest {
            val fakeRepository = FakeDeviceRepository()
            val event1 = TestDevices.createBatteryEvent(
                id = "e1",
                deviceId = "d1",
                date = Instant.parse("2024-01-01T00:00:00Z"),
            )
            val event2 = TestDevices.createBatteryEvent(
                id = "e2",
                deviceId = "d2",
                date = Instant.parse("2024-02-01T00:00:00Z"),
            )
            val event3 = TestDevices.createBatteryEvent(
                id = "e3",
                deviceId = "d1",
                date = Instant.parse("2024-03-01T00:00:00Z"),
            )
            fakeRepository.setEvents(listOf(event1, event2, event3))
            val useCase = GetBatteryEventsUseCase(fakeRepository)

            val result = useCase.forDevice("d1").first()

            assertEquals(2, result.size)
            assertTrue(result.all { it.deviceId == "d1" })
            assertTrue(result.contains(event1))
            assertTrue(result.contains(event3))
        }
}
