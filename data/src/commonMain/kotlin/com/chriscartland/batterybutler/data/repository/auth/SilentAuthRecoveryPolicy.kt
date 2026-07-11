package com.chriscartland.batterybutler.data.repository.auth

import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.AuthState

/**
 * Gate for how often a best-effort silent re-auth attempt is allowed to run.
 *
 * The "silent" Google Sign-In call ([com.chriscartland.batterybutler.datanetwork.auth.GoogleSignInBridge.signInSilentlyWithClient])
 * is not actually guaranteed headless -- on Android it uses the same Credential Manager
 * `getCredential` call as the interactive picker, differing only by `filterByAuthorizedAccounts`.
 * With multiple accounts on-device, or when Play Services decides a credential needs
 * re-confirmation, that "silent" call can still surface a chooser/bottom-sheet. Since
 * [DefaultLabsAuthRepository] is recreated fresh on every process (re)start and attempts this once
 * per (re)start, killing/relaunching the app frequently multiplied how often that OS UI could
 * appear -- see `bb-silent-reauth-cooldown` in TODO.md. Throttling attempts trades a longer window
 * before an automatically-restored session (harmless: the caller already tolerates the pre-existing
 * session staying unrestored until the next attempt or an explicit sign-in) for far fewer surprise
 * dialogs.
 */
internal object SilentReauthCooldown {
    const val COOLDOWN_MS = 6 * 60 * 60 * 1000L

    fun shouldAttempt(
        lastAttemptAtMs: Long?,
        nowMs: Long,
    ): Boolean = lastAttemptAtMs == null || nowMs - lastAttemptAtMs >= COOLDOWN_MS
}

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
