package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.User
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.FakeLabsAuthRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SignOutLabsUseCaseTest {
    @Test
    fun `invoke signs out of Labs and clears local data`() =
        runTest {
            val labsAuthRepository = FakeLabsAuthRepository()
            val user = User(id = "labs-user", email = "labs@example.com", displayName = "Labs User", photoUrl = null)
            labsAuthRepository.setLabsAuthState(AuthState.Authenticated(user))
            val deviceRepository = FakeDeviceRepository()
            deviceRepository.setDevices(listOf(TestDevices.createDevice(id = "1")))
            val useCase = SignOutLabsUseCase(labsAuthRepository, deviceRepository)

            useCase()

            assertEquals(1, labsAuthRepository.signOutCount)
            assertIs<AuthState.Unauthenticated>(labsAuthRepository.labsAuthState.first())
            assertEquals(1, deviceRepository.clearAllLocalDataCount)
            assertTrue(deviceRepository.getAllDevices().first().isEmpty())
        }

    @Test
    fun `invoke clears local data even when nothing was signed in`() =
        runTest {
            val labsAuthRepository = FakeLabsAuthRepository()
            val deviceRepository = FakeDeviceRepository()
            deviceRepository.setDeviceTypes(listOf(TestDevices.createDeviceType(id = "t1")))
            val useCase = SignOutLabsUseCase(labsAuthRepository, deviceRepository)

            useCase()

            assertEquals(1, deviceRepository.clearAllLocalDataCount)
            assertTrue(deviceRepository.getAllDeviceTypes().first().isEmpty())
        }
}
