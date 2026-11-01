package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.User
import com.chriscartland.batterybutler.testcommon.FakeAuthRepository
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.FakeNeedsBatteryRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SignOutUseCaseTest {
    @Test
    fun `invoke signs out and clears local data`() =
        runTest {
            val authRepository = FakeAuthRepository()
            val user = User(id = "user-1", email = "user@example.com", displayName = "Test User", photoUrl = null)
            authRepository.setAuthState(AuthState.Authenticated(user))
            val deviceRepository = FakeDeviceRepository()
            deviceRepository.setDevices(listOf(TestDevices.createDevice(id = "1")))
            val useCase = SignOutUseCase(authRepository, deviceRepository, FakeNeedsBatteryRepository())

            useCase()

            assertEquals(1, authRepository.signOutCallCount)
            assertIs<AuthState.Unauthenticated>(authRepository.authState.first())
            assertEquals(1, deviceRepository.clearAllLocalDataCount)
            assertTrue(deviceRepository.getAllDevices().first().isEmpty())
        }

    @Test
    fun `invoke clears local data even when nothing was signed in`() =
        runTest {
            val authRepository = FakeAuthRepository()
            val deviceRepository = FakeDeviceRepository()
            deviceRepository.setDeviceTypes(listOf(TestDevices.createDeviceType(id = "t1")))
            val useCase = SignOutUseCase(authRepository, deviceRepository, FakeNeedsBatteryRepository())

            useCase()

            assertEquals(1, deviceRepository.clearAllLocalDataCount)
            assertTrue(deviceRepository.getAllDeviceTypes().first().isEmpty())
        }

    /**
     * Needs-battery marks live outside the synced tables, so `clearAllLocalData()` doesn't reach
     * them -- without this the next account to sign in would inherit the last one's marks.
     */
    @Test
    fun `invoke clears needs-battery marks`() =
        runTest {
            val needsBattery = FakeNeedsBatteryRepository(initialFlagged = setOf("d1", "d2"))

            SignOutUseCase(FakeAuthRepository(), FakeDeviceRepository(), needsBattery)()

            assertEquals(1, needsBattery.clearAllCount)
            assertTrue(needsBattery.getFlaggedDeviceIds().first().isEmpty())
        }
}
