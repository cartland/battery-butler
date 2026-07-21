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
import org.jetbrains.compose.resources.StringResource

/**
 * Title + message string resources for one Labs sign-in failure. A plain (non-composable) pair of
 * resource references so the [AuthError] -> copy mapping is pure and unit-testable; callers
 * resolve the resources with `composeStringResource` at render time.
 */
data class LabsAuthErrorText(
    val title: StringResource,
    val message: StringResource,
)

/**
 * Maps a Labs sign-in [AuthError] to Labs-specific copy. The legacy `auth_error_*` strings were
 * written for the own-backend flow ("Coming Soon" for its unconfigured OAuth, "Session Expired"
 * for its server session) and read wrong for Labs — e.g. an unconfigured Labs OAuth client is a
 * build-configuration gap, not an upcoming feature.
 */
fun labsAuthErrorText(error: AuthError): LabsAuthErrorText =
    when (error) {
        is AuthError.Configuration.NotConfigured -> LabsAuthErrorText(
            title = Res.string.labs_auth_error_title_not_configured,
            message = Res.string.labs_auth_error_message_not_configured,
        )

        is AuthError.Configuration.ServerUnavailable, is AuthError.SignIn.NetworkError -> LabsAuthErrorText(
            title = Res.string.labs_auth_error_title_network,
            message = Res.string.labs_auth_error_message_network,
        )

        is AuthError.SignIn.Cancelled -> LabsAuthErrorText(
            title = Res.string.labs_auth_error_title_cancelled,
            message = Res.string.labs_auth_error_message_cancelled,
        )

        is AuthError.Token.Invalid, is AuthError.Token.Expired -> LabsAuthErrorText(
            title = Res.string.labs_auth_error_title_session_expired,
            message = Res.string.labs_auth_error_message_session_expired,
        )

        is AuthError.SignIn.Failed, is AuthError.Unknown -> LabsAuthErrorText(
            title = Res.string.labs_auth_error_title_failed,
            message = Res.string.labs_auth_error_message_failed,
        )
    }

/**
 * True when [state] is the *reactive* session-expired sign-out — the backend authoritatively
 * rejected the stored Labs session ([SignedOutCause.SESSION_EXPIRED]) — as opposed to the plain
 * signed-out resting state. Only the Labs auth repository ever sets this cause, so UI gated on it
 * can safely show Labs-specific "session expired" copy.
 */
fun isLabsSessionExpired(state: AuthState): Boolean = state is AuthState.Unauthenticated && state.cause == SignedOutCause.SESSION_EXPIRED
