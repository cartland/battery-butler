package com.chriscartland.batterybutler.domain.model

/**
 * Terminal outcome of a single explicit sync attempt
 * ([com.chriscartland.batterybutler.domain.repository.DeviceRepository.resync]).
 *
 * [SyncStatus] is the *observable stream* the UI watches; this is the *return value* a caller
 * gets, so call sites (sign-in's immediate resync, pull-to-refresh) can branch on what actually
 * happened instead of every failure masquerading as a silent success or timeout.
 */
sealed interface SyncOutcome {
    /** A remote snapshot was fetched and applied locally. */
    data object Success : SyncOutcome

    /** No remote is configured ([DataMode.None]); there was nothing to sync. */
    data object Skipped : SyncOutcome

    /**
     * The backend requires a (new) sign-in. Nothing was applied locally and local data is
     * untouched. [reason] carries the parsed cause for logging/copy.
     */
    data class AuthRequired(
        val reason: SyncAuthReason,
    ) : SyncOutcome

    /** The sync attempt failed (network, server, timeout, or unexpected error). */
    data class Failed(
        val error: DataError,
    ) : SyncOutcome
}
