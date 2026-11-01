package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.testcommon.FakeNeedsBatteryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SetDeviceNeedsBatteryUseCaseTest {
    @Test
    fun `marking a device makes it flagged`() =
        runTest {
            val needsBattery = FakeNeedsBatteryRepository()

            SetDeviceNeedsBatteryUseCase(needsBattery)("d1", needsBattery = true)

            assertEquals(setOf("d1"), needsBattery.getFlaggedDeviceIds().first())
        }

    @Test
    fun `unmarking a device removes only that device`() =
        runTest {
            val needsBattery = FakeNeedsBatteryRepository(initialFlagged = setOf("d1", "d2"))

            SetDeviceNeedsBatteryUseCase(needsBattery)("d1", needsBattery = false)

            assertEquals(setOf("d2"), needsBattery.getFlaggedDeviceIds().first())
        }

    @Test
    fun `marking the same device twice leaves a single mark`() =
        runTest {
            val needsBattery = FakeNeedsBatteryRepository()
            val useCase = SetDeviceNeedsBatteryUseCase(needsBattery)

            useCase("d1", needsBattery = true)
            useCase("d1", needsBattery = true)

            assertEquals(setOf("d1"), needsBattery.getFlaggedDeviceIds().first())
        }

    @Test
    fun `unmarking a device that was never marked is a no-op`() =
        runTest {
            val needsBattery = FakeNeedsBatteryRepository(initialFlagged = setOf("d1"))

            SetDeviceNeedsBatteryUseCase(needsBattery)("d2", needsBattery = false)

            assertEquals(setOf("d1"), needsBattery.getFlaggedDeviceIds().first())
        }
}
