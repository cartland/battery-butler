package com.chriscartland.batterybutler.data.repository.auth

import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.AuthState
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthStateForServerRejectionTest {
    private val error = AuthError.Token.Invalid(message = "Server rejected token", cause = "bad token")

    @Test
    fun `explicit attempt fails loudly`() {
        assertEquals(AuthState.Failed(error), authStateForServerRejection(isExplicitAttempt = true, error = error))
    }

    @Test
    fun `background attempt fails quietly`() {
        assertEquals(AuthState.Unauthenticated(), authStateForServerRejection(isExplicitAttempt = false, error = error))
    }
}
