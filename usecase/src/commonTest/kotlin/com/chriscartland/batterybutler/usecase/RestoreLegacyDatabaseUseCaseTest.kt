package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.testcommon.FakeLegacyDatabaseRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RestoreLegacyDatabaseUseCaseTest {
    @Test
    fun `invoke delegates to repository`() =
        runTest {
            val repo = FakeLegacyDatabaseRepository()
            val useCase = RestoreLegacyDatabaseUseCase(repo)

            useCase("battery-butler.db")

            assertEquals(1, repo.restoreCallCount)
        }

    @Test
    fun `passes correct file name to repository`() =
        runTest {
            val repo = FakeLegacyDatabaseRepository()
            val useCase = RestoreLegacyDatabaseUseCase(repo)

            useCase("battery-butler-dev.db")

            assertEquals("battery-butler-dev.db", repo.lastRestoredFileName)
        }
}
