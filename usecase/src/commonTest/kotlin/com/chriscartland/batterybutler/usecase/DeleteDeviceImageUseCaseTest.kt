package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.testcommon.FakeDeviceImageRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeleteDeviceImageUseCaseTest {
    @Test
    fun `delegates to the repository`() =
        runTest {
            val repo = FakeDeviceImageRepository()
            val useCase = DeleteDeviceImageUseCase(repo)

            val result = useCase("dev1")

            assertTrue(result)
            assertEquals(listOf("dev1"), repo.deletedDeviceIds)
        }

    @Test
    fun `propagates a repository failure`() =
        runTest {
            val repo = FakeDeviceImageRepository().apply { deleteResult = false }
            val useCase = DeleteDeviceImageUseCase(repo)

            assertEquals(false, useCase("dev1"))
        }
}
