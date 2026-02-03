package com.chriscartland.batterybutler.domain.model

/**
 * Represents the current state of repository synchronization with remote storage.
 *
 * The sync status follows a state machine pattern:
 * - [Idle] -> [Syncing] -> [Success] or [Failed]
 * - [Success]/[Failed] -> [Idle] (after UI dismissal)
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
}
