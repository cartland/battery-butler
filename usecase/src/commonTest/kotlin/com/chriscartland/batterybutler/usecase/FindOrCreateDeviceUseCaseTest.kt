package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.getOrThrow
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class FindOrCreateDeviceUseCaseTest {
    @Test
    fun `returns existing device when name matches`() =
        runTest {
            val repo = FakeDeviceRepository()
            val findOrCreateType = FindOrCreateDeviceTypeUseCase(repo)
            val useCase = FindOrCreateDeviceUseCase(repo, findOrCreateType)
            repo.setDevices(
                listOf(TestDevices.createDevice(id = "device-1", name = "Smoke Detector")),
            )

            val result = useCase("Smoke Detector").getOrThrow()

            assertEquals("Smoke Detector", result.name)
            assertEquals(1, repo.devices.size)
        }

    @Test
    fun `creates new device when name not found`() =
        runTest {
            val repo = FakeDeviceRepository()
            val findOrCreateType = FindOrCreateDeviceTypeUseCase(repo)
            val useCase = FindOrCreateDeviceUseCase(repo, findOrCreateType)

            useCase("New Device")

            assertEquals(1, repo.devices.size)
            assertEquals("New Device", repo.devices[0].name)
        }

    @Test
    fun `creates device with resolved type`() =
        runTest {
            val repo = FakeDeviceRepository()
            val findOrCreateType = FindOrCreateDeviceTypeUseCase(repo)
            val useCase = FindOrCreateDeviceUseCase(repo, findOrCreateType)
            repo.setDeviceTypes(
                listOf(TestDevices.createDeviceType(id = "type-sensor", name = "Sensor")),
            )

            val result = useCase("My Sensor", "Sensor").getOrThrow()

            assertEquals("type-sensor", result.typeId)
        }

    @Test
    fun `null type uses default`() =
        runTest {
            val repo = FakeDeviceRepository()
            val findOrCreateType = FindOrCreateDeviceTypeUseCase(repo)
            val useCase = FindOrCreateDeviceUseCase(repo, findOrCreateType)

            val result = useCase("Device Without Type", null).getOrThrow()

            assertEquals("default_type", result.typeId)
        }
}
