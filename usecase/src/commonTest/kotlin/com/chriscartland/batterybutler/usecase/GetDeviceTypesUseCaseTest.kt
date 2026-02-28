package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class GetDeviceTypesUseCaseTest {
    @Test
    fun `returns flow from repository`() =
        runTest {
            val fakeRepository = FakeDeviceRepository()
            val type1 = TestDevices.createDeviceType(id = "t1", name = "Smoke Detector")
            val type2 = TestDevices.createDeviceType(id = "t2", name = "Remote Control")
            fakeRepository.setDeviceTypes(listOf(type1, type2))
            val useCase = GetDeviceTypesUseCase(fakeRepository)

            val result = useCase().first()

            assertEquals(2, result.size)
            assertEquals(type1, result[0])
            assertEquals(type2, result[1])
        }

    @Test
    fun `emits updated list when repository changes`() =
        runTest {
            val fakeRepository = FakeDeviceRepository()
            val type1 = TestDevices.createDeviceType(id = "t1", name = "Smoke Detector")
            fakeRepository.setDeviceTypes(listOf(type1))
            val useCase = GetDeviceTypesUseCase(fakeRepository)

            val firstResult = useCase().first()
            assertEquals(1, firstResult.size)

            val type2 = TestDevices.createDeviceType(id = "t2", name = "Remote Control")
            fakeRepository.setDeviceTypes(listOf(type1, type2))

            val secondResult = useCase().first()
            assertEquals(2, secondResult.size)
            assertTrue(secondResult.contains(type1))
            assertTrue(secondResult.contains(type2))
        }
}
