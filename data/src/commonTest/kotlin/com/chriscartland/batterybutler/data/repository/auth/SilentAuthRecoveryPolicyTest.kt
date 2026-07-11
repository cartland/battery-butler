package com.chriscartland.batterybutler.data.repository.auth

import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.AuthState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SilentReauthCooldownTest {
    @Test
    fun `never attempted before allows an attempt`() {
        assertTrue(SilentReauthCooldown.shouldAttempt(lastAttemptAtMs = null, nowMs = 1_000L))
    }

    @Test
    fun `attempt within the cooldown window is blocked`() {
        val last = 10_000L
        val now = last + SilentReauthCooldown.COOLDOWN_MS - 1
        assertFalse(SilentReauthCooldown.shouldAttempt(lastAttemptAtMs = last, nowMs = now))
    }

    @Test
    fun `attempt exactly at the cooldown boundary is allowed`() {
        val last = 10_000L
        val now = last + SilentReauthCooldown.COOLDOWN_MS
        assertTrue(SilentReauthCooldown.shouldAttempt(lastAttemptAtMs = last, nowMs = now))
    }

    @Test
    fun `attempt well past the cooldown window is allowed`() {
        val last = 10_000L
        val now = last + SilentReauthCooldown.COOLDOWN_MS + 999_999L
        assertTrue(SilentReauthCooldown.shouldAttempt(lastAttemptAtMs = last, nowMs = now))
    }
}

class AuthStateForServerRejectionTest {
    private val error = AuthError.Token.Invalid(message = "Server rejected token", cause = "bad token")

    @Test
    fun `explicit attempt fails loudly`() {
        assertEquals(AuthState.Failed(error), authStateForServerRejection(isExplicitAttempt = true, error = error))
    }

    @Test
    fun `background attempt fails quietly`() {
        assertEquals(AuthState.Unauthenticated, authStateForServerRejection(isExplicitAttempt = false, error = error))
    }
}
