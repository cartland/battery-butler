package com.chriscartland.batterybutler.domain.model

/**
 * Why a sync attempt needs (re-)authentication.
 *
 * Mirrors the Labs backend's 401 `error.details.reason` values ("expired", "invalid") plus the
 * client-side case of having no session at all. The reason is advisory detail for logging and
 * copy; the *state* is always the same — the user must sign in before sync can proceed.
 */
enum class SyncAuthReason {
    /** No Labs session exists on this device (no ID token available); no request was sent. */
    NO_SESSION,

    /** The backend reported the presented token as expired (`details.reason == "expired"`). */
    TOKEN_EXPIRED,

    /** The backend reported the presented token as invalid (`details.reason == "invalid"`). */
    TOKEN_INVALID,

    /** A 401 without a parseable reason (older backend, absent details, non-JSON error body). */
    UNKNOWN,
}
