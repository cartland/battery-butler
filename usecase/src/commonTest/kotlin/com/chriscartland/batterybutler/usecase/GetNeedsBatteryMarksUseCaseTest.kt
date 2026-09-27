package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.testcommon.FakeNeedsBatteryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class GetNeedsBatteryMarksUseCaseTest {
    @Test
    fun `returns each mark with the moment it was made`() =
        runTest {
            val marks = mapOf("d1" to Instant.fromEpochMilliseconds(1_000), "d2" to Instant.fromEpochMilliseconds(2_000))

            val result = GetNeedsBatteryMarksUseCase(FakeNeedsBatteryRepository(initialMarks = marks))().first()

            assertEquals(marks, result)
        }

    @Test
    fun `is empty when nothing is marked`() =
        runTest {
            val result = GetNeedsBatteryMarksUseCase(FakeNeedsBatteryRepository())().first()

            assertTrue(result.isEmpty())
        }

    /** The timestamp is the whole point of this use case existing alongside the ids one. */
    @Test
    fun `marking a device records when it was marked`() =
        runTest {
            val repo = FakeNeedsBatteryRepository()
            repo.now = Instant.fromEpochMilliseconds(4_242)

            repo.setNeedsBattery("d1", needsBattery = true)

            assertEquals(
                mapOf("d1" to Instant.fromEpochMilliseconds(4_242)),
                GetNeedsBatteryMarksUseCase(repo)().first(),
            )
        }

    @Test
    fun `clearing a mark drops it from the map`() =
        runTest {
            val repo = FakeNeedsBatteryRepository(initialMarks = mapOf("d1" to Instant.fromEpochMilliseconds(1)))

            repo.setNeedsBattery("d1", needsBattery = false)

            assertTrue(GetNeedsBatteryMarksUseCase(repo)().first().isEmpty())
        }
}
