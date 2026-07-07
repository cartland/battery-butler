package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.FakeLabsAuthRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SignInToLabsUseCaseTest {
    @Test
    fun `invoke triggers a resync on successful sign-in`() =
        runTest {
            val labsAuthRepository = FakeLabsAuthRepository()
            val deviceRepository = FakeDeviceRepository()
            val useCase = SignInToLabsUseCase(labsAuthRepository, deviceRepository)

            val result = useCase()

            assertEquals(1, labsAuthRepository.signInCount)
            assertIs<Result.Success<*>>(result)
            assertEquals(1, deviceRepository.resyncCount)
        }

    @Test
    fun `invoke does not resync when sign-in fails`() =
        runTest {
            val labsAuthRepository = FakeLabsAuthRepository()
            labsAuthRepository.signInResult = Result.Error(
                AuthError.Configuration.NotConfigured(message = "not configured", cause = "test"),
            )
            val deviceRepository = FakeDeviceRepository()
            val useCase = SignInToLabsUseCase(labsAuthRepository, deviceRepository)

            val result = useCase()

            assertEquals(1, labsAuthRepository.signInCount)
            assertIs<Result.Error<*>>(result)
            assertEquals(0, deviceRepository.resyncCount)
        }
}
