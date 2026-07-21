package com.chriscartland.batterybutler.domain.model

/**
 * Represents the current state of repository synchronization with remote storage.
 *
 * The sync status follows a state machine pattern:
 * - [Idle] -> [Syncing] -> [Success], [Failed], or [AuthRequired]
 * - [Success]/[Failed]/[AuthRequired] -> [Idle] (after UI dismissal)
 *
 * This status is exposed via [DeviceRepository.syncStatus] and observed by the UI
 * to show sync indicators and error messages.
 */
sealed interface SyncStatus {
    /** No sync operation in progress. Default resting state. */
    data object Idle : SyncStatus

    /** A sync operation is currently in progress. */
    data object Syncing : SyncStatus

    /** The last sync operation completed successfully. */
    data object Success : SyncStatus

    /**
     * The last sync operation failed.
     * @property error Typed error with message and cause for programmatic handling.
     *                 Use [DataError.message] for display text.
     */
    data class Failed(
        val error: DataError,
    ) : SyncStatus

    /**
     * The backend refused the last sync for lack of a valid session (or no session exists on
     * this device at all). Local data is untouched and the background sync loop keeps retrying
     * with its normal backoff. Distinct from [Failed] so the UI can prompt for sign-in rather
     * than show a generic sync error.
     *
     * @property reason The parsed cause (expired/invalid token, no session, or unknown).
     */
    data class AuthRequired(
        val reason: SyncAuthReason,
    ) : SyncStatus
}
