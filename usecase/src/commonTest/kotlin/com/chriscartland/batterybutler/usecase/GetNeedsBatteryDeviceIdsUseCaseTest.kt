package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.testcommon.FakeNeedsBatteryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetNeedsBatteryDeviceIdsUseCaseTest {
    @Test
    fun `emits nothing when no device is marked`() =
        runTest {
            val useCase = GetNeedsBatteryDeviceIdsUseCase(FakeNeedsBatteryRepository())

            assertTrue(useCase().first().isEmpty())
        }

    @Test
    fun `emits every marked device`() =
        runTest {
            val useCase = GetNeedsBatteryDeviceIdsUseCase(
                FakeNeedsBatteryRepository(initialFlagged = setOf("d1", "d2")),
            )

            assertEquals(setOf("d1", "d2"), useCase().first())
        }

    /** The list UI depends on this being reactive -- a badge has to appear without a reload. */
    @Test
    fun `re-emits when a mark is added`() =
        runTest {
            val repo = FakeNeedsBatteryRepository()
            val useCase = GetNeedsBatteryDeviceIdsUseCase(repo)

            repo.setNeedsBattery("d1", needsBattery = true)

            assertEquals(setOf("d1"), useCase().first())
        }
}
