package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.testcommon.FakeDeviceImageRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsDeviceImagesSupportedUseCaseTest {
    @Test
    fun `emits false by default`() =
        runTest {
            val useCase = IsDeviceImagesSupportedUseCase(FakeDeviceImageRepository())
            assertFalse(useCase().first())
        }

    @Test
    fun `emits true when the repository reports supported`() =
        runTest {
            val repo = FakeDeviceImageRepository().apply { setSupported(true) }
            val useCase = IsDeviceImagesSupportedUseCase(repo)
            assertTrue(useCase().first())
        }
}
