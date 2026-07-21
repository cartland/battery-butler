package com.chriscartland.batterybutler.domain.model

/**
 * How a Labs environment's one-shot cold-start session restore resolved.
 *
 * Returned by [com.chriscartland.batterybutler.domain.repository.LabsAuthRepository
 * .awaitLabsSessionRestore], the gate the sync layer awaits before firing its first Labs request
 * on a cold start. Every value is *resolved* — sync may proceed once any of them is reached; the
 * distinctions exist for logging and tests, not for gating differently:
 *
 * - [RESTORED] and [NO_SESSION] are the happy paths (a live session, or definitively nothing to
 *   restore).
 * - [INVALID] means the persisted refresh token was authoritatively rejected; the reactive
 *   session-loss path (state flip + credential clear) has already run.
 * - [TRANSIENT_FAILURE] means the restore attempt failed for network-ish reasons. Sync proceeds
 *   anyway and surfaces *network* errors (never a spurious "sign in required"): the token path
 *   re-attempts the restore on demand, so a recovered network heals without user action.
 */
enum class LabsSessionRestoreResult {
    /** A live session exists — restored from the persisted refresh token, or freshly signed in. */
    RESTORED,

    /** No persisted session for this environment (never signed in, or signed out). Definitive. */
    NO_SESSION,

    /** The persisted refresh token was authoritatively rejected. Definitive; session loss handled. */
    INVALID,

    /** The restore attempt failed transiently (network unreachable, server error). Not definitive. */
    TRANSIENT_FAILURE,
}
