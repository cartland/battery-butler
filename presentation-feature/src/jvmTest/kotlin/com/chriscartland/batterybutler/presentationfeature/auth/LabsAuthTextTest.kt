package com.chriscartland.batterybutler.presentationfeature.auth

import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.labs_auth_error_message_cancelled
import com.chriscartland.batterybutler.composeresources.generated.resources.labs_auth_error_message_failed
import com.chriscartland.batterybutler.composeresources.generated.resources.labs_auth_error_message_network
import com.chriscartland.batterybutler.composeresources.generated.resources.labs_auth_error_message_not_configured
import com.chriscartland.batterybutler.composeresources.generated.resources.labs_auth_error_message_session_expired
import com.chriscartland.batterybutler.composeresources.generated.resources.labs_auth_error_title_cancelled
import com.chriscartland.batterybutler.composeresources.generated.resources.labs_auth_error_title_failed
import com.chriscartland.batterybutler.composeresources.generated.resources.labs_auth_error_title_network
import com.chriscartland.batterybutler.composeresources.generated.resources.labs_auth_error_title_not_configured
import com.chriscartland.batterybutler.composeresources.generated.resources.labs_auth_error_title_session_expired
import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.SignedOutCause
import com.chriscartland.batterybutler.domain.model.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the Labs [AuthError] -> string-resource mapping: every variant resolves to Labs-specific
 * copy (never the legacy own-backend `auth_error_*` strings, whose "Coming Soon" / "Session
 * Expired" text was written for the gRPC flow), and the session-expired helper only fires for the
 * reactive [SignedOutCause.SESSION_EXPIRED] sign-out.
 */
class LabsAuthTextTest {
    @Test
    fun `an unconfigured Labs environment maps to the not-configured copy`() {
        val text = labsAuthErrorText(AuthError.Configuration.NotConfigured())
        assertEquals(Res.string.labs_auth_error_title_not_configured, text.title)
        assertEquals(Res.string.labs_auth_error_message_not_configured, text.message)
    }

    @Test
    fun `an unreachable auth server maps to the network copy`() {
        val text = labsAuthErrorText(AuthError.Configuration.ServerUnavailable())
        assertEquals(Res.string.labs_auth_error_title_network, text.title)
        assertEquals(Res.string.labs_auth_error_message_network, text.message)
    }

    @Test
    fun `a cancelled sign-in maps to the reassuring cancelled copy`() {
        val text = labsAuthErrorText(AuthError.SignIn.Cancelled())
        assertEquals(Res.string.labs_auth_error_title_cancelled, text.title)
        assertEquals(Res.string.labs_auth_error_message_cancelled, text.message)
    }

    @Test
    fun `a network failure during sign-in maps to the network copy`() {
        val text = labsAuthErrorText(AuthError.SignIn.NetworkError())
        assertEquals(Res.string.labs_auth_error_title_network, text.title)
        assertEquals(Res.string.labs_auth_error_message_network, text.message)
    }

    @Test
    fun `a failed sign-in maps to the generic Labs failure copy`() {
        val text = labsAuthErrorText(AuthError.SignIn.Failed(cause = "signInWithIdp HTTP 500"))
        assertEquals(Res.string.labs_auth_error_title_failed, text.title)
        assertEquals(Res.string.labs_auth_error_message_failed, text.message)
    }

    @Test
    fun `token errors map to the session-expired copy`() {
        val invalid = labsAuthErrorText(AuthError.Token.Invalid())
        assertEquals(Res.string.labs_auth_error_title_session_expired, invalid.title)
        assertEquals(Res.string.labs_auth_error_message_session_expired, invalid.message)

        val expired = labsAuthErrorText(AuthError.Token.Expired())
        assertEquals(Res.string.labs_auth_error_title_session_expired, expired.title)
        assertEquals(Res.string.labs_auth_error_message_session_expired, expired.message)
    }

    @Test
    fun `an unknown error maps to the generic Labs failure copy`() {
        val text = labsAuthErrorText(AuthError.Unknown())
        assertEquals(Res.string.labs_auth_error_title_failed, text.title)
        assertEquals(Res.string.labs_auth_error_message_failed, text.message)
    }

    @Test
    fun `isLabsSessionExpired is true only for the reactive session-expired sign-out`() {
        assertTrue(
            isLabsSessionExpired(AuthState.Unauthenticated(cause = SignedOutCause.SESSION_EXPIRED)),
        )
        assertFalse(
            isLabsSessionExpired(AuthState.Unauthenticated()),
            "the plain signed-out resting state must not show session-expired copy",
        )
        assertFalse(isLabsSessionExpired(AuthState.Unknown))
        assertFalse(
            isLabsSessionExpired(
                AuthState.Authenticated(
                    User(id = "uid", email = null, displayName = null, photoUrl = null),
                ),
            ),
        )
        assertFalse(isLabsSessionExpired(AuthState.Failed(AuthError.Unknown())))
    }
}
