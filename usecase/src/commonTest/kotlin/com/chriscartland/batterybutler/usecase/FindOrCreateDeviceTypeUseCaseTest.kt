package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class FindOrCreateDeviceTypeUseCaseTest {
    @Test
    fun `returns existing type ID when name matches`() =
        runTest {
            val repo = FakeDeviceRepository()
            val useCase = FindOrCreateDeviceTypeUseCase(repo)
            repo.setDeviceTypes(
                listOf(TestDevices.createDeviceType(id = "type-1", name = "Smoke Detector")),
            )

            val result = useCase("Smoke Detector")

            assertEquals(Result.Success("type-1"), result)
        }

    @Test
    fun `creates new type with default icon when name not found`() =
        runTest {
            val repo = FakeDeviceRepository()
            val useCase = FindOrCreateDeviceTypeUseCase(repo)

            useCase("New Type")

            assertEquals(1, repo.deviceTypes.size)
            assertEquals("New Type", repo.deviceTypes[0].name)
            assertEquals("default", repo.deviceTypes[0].defaultIcon)
        }

    @Test
    fun `returns default_type for null name`() =
        runTest {
            val repo = FakeDeviceRepository()
            val useCase = FindOrCreateDeviceTypeUseCase(repo)

            val result = useCase(null)

            assertEquals(Result.Success("default_type"), result)
            assertTrue(repo.deviceTypes.isEmpty())
        }

    @Test
    fun `returns default_type for blank name`() =
        runTest {
            val repo = FakeDeviceRepository()
            val useCase = FindOrCreateDeviceTypeUseCase(repo)

            val result = useCase("  ")

            assertEquals(Result.Success("default_type"), result)
        }
}
