package com.chriscartland.batterybutler.data.repository.auth

import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.AuthState

/**
 * Decides the [AuthState] a rejected-by-server token should land on, distinguishing an explicit,
 * user-initiated sign-in from a background silent-refresh attempt ([DefaultAuthRepository.attemptSilentRefresh]).
 * An explicit attempt drops to [AuthState.Failed], which [LoginContent] auto-shows as an error
 * dialog the actively-waiting user can retry from. A background attempt fails quietly to
 * [AuthState.Unauthenticated] instead -- the same tolerance already given to a failed
 * [com.chriscartland.batterybutler.datanetwork.auth.GoogleSignInBridge.signInSilently] call -- so a
 * routine background refresh cycle can't leave the user with an unprompted error dialog whose only
 * action opens the interactive picker. See `bb-silent-reauth-cooldown` in TODO.md.
 */
internal fun authStateForServerRejection(
    isExplicitAttempt: Boolean,
    error: AuthError,
): AuthState = if (isExplicitAttempt) AuthState.Failed(error) else AuthState.Unauthenticated
