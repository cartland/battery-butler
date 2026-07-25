package com.chriscartland.batterybutler.domain.model

/**
 * Why an [AuthState.Unauthenticated] state was entered.
 *
 * Modeled as a narrow enum (not a boolean) so a *reactive* session loss — the backend
 * authoritatively rejecting the stored session, with the user having done nothing — stays
 * distinguishable from the ordinary signed-out resting state. A reactive loss keeps local data
 * (the user did not sign out; only [com.chriscartland.batterybutler.domain.repository
 * .LabsAuthRepository.signOutLabs] via its use case clears the local cache), so the UI may want
 * different copy ("session expired, sign in again") without any structural difference in what
 * the state permits.
 */
enum class SignedOutCause {
    /** Never signed in on this environment, or the user explicitly signed out. */
    SIGNED_OUT,

    /**
     * The backend authoritatively rejected the stored session (refresh token revoked, or a
     * freshly-refreshed token still refused). The persisted credentials were cleared; local
     * device data was deliberately left untouched.
     */
    SESSION_EXPIRED,
}
