package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.CommonDeviceTypes
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class PreloadCommonTypesUseCaseTest {
    @Test
    fun `adds all common types to empty repository`() =
        runTest {
            val repo = FakeDeviceRepository()
            val useCase = PreloadCommonTypesUseCase(repo)

            useCase()

            assertEquals(CommonDeviceTypes.types.size, repo.deviceTypes.size)
        }

    @Test
    fun `skips existing types by name`() =
        runTest {
            val repo = FakeDeviceRepository()
            val useCase = PreloadCommonTypesUseCase(repo)
            repo.setDeviceTypes(
                listOf(TestDevices.createDeviceType(id = "existing-1", name = "Smart Button")),
            )

            useCase()

            assertEquals(CommonDeviceTypes.types.size, repo.deviceTypes.size)
            val existingType = repo.deviceTypes.find { it.id == "existing-1" }
            assertNotNull(existingType)
            assertEquals("Smart Button", existingType.name)
        }

    @Test
    fun `preserves battery info from templates`() =
        runTest {
            val repo = FakeDeviceRepository()
            val useCase = PreloadCommonTypesUseCase(repo)

            useCase()

            val lockType = repo.deviceTypes.find { it.name == "ULTRALOQ U-Bolt Pro Lock" }
            assertNotNull(lockType)
            assertEquals("AA", lockType.batteryType)
            assertEquals(4, lockType.batteryQuantity)
        }
}
