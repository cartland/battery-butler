package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.FakeLabsAuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
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
            val useCase = SignInToLabsUseCase(labsAuthRepository, deviceRepository, backgroundScope)

            val result = useCase()
            // The resync is launched on the app scope, not awaited inline. runCurrent (not
            // advanceUntilIdle): background-scope-only tasks don't count toward "idle".
            runCurrent()

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
            val useCase = SignInToLabsUseCase(labsAuthRepository, deviceRepository, backgroundScope)

            val result = useCase()
            runCurrent()

            assertEquals(1, labsAuthRepository.signInCount)
            assertIs<Result.Error<*>>(result)
            assertEquals(0, deviceRepository.resyncCount)
        }

    /**
     * The post-sign-in resync must run on the app-lifetime scope, not the caller's: sign-in flips
     * the auth state, the UI navigates, and the navigation tears the caller's viewModelScope down
     * — which used to kill the resync mid-flight (documented residual of `bb-signin-empty-list`).
     * This cancels the caller right after invoke() returns and asserts the resync still completed.
     * Against the pre-fix code (resync awaited inline on the caller) this test fails: the
     * cancellation aborts the in-flight resync and `resyncCompletedCount` stays 0.
     */
    @Test
    fun `resync survives cancellation of the calling scope`() =
        runTest {
            val labsAuthRepository = FakeLabsAuthRepository()
            val deviceRepository = FakeDeviceRepository()
            val resyncGate = CompletableDeferred<Unit>()
            deviceRepository.resyncBody = { resyncGate.await() }
            val useCase = SignInToLabsUseCase(labsAuthRepository, deviceRepository, backgroundScope)

            // Stand-in for the LoginViewModel's viewModelScope.
            val callerJob: Job = launch { useCase() }
            runCurrent() // sign-in completes; the resync is launched and suspends on the gate

            callerJob.cancel() // navigation tears the caller down while the resync is in flight
            runCurrent()

            resyncGate.complete(Unit)
            runCurrent() // (not advanceUntilIdle — background-scope-only tasks don't count toward "idle")

            assertEquals(1, deviceRepository.resyncCount, "resync must have started")
            assertEquals(
                1,
                deviceRepository.resyncCompletedCount,
                "the resync must complete despite the caller's cancellation",
            )
        }
}
