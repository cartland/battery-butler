package com.chriscartland.batterybutler.domain.model

/**
 * Outcome of restoring a legacy database file via
 * [com.chriscartland.batterybutler.domain.repository.LegacyDatabaseRepository.restoreLegacyDatabase].
 *
 * The repository never throws for any of these outcomes — they are all values
 * the UI can surface. Truly unexpected errors (out-of-disk, OS-level IO) are
 * reported as [Failure] with the exception captured.
 */
sealed interface RestoreResult {
    /**
     * Restore completed: legacy file copied over the active database file,
     * Room reopened cleanly, [com.chriscartland.batterybutler.domain.model.DataMode]-bound
     * flows have been re-bound. Data should be visible across all tabs.
     */
    data object Success : RestoreResult

    /**
     * Room opened the restored file but had to fall back to a destructive
     * migration because the file was at an unsupported schema version (e.g.,
     * a pre-Room raw SQLite export). The active database is now a fresh empty
     * v5 schema — the legacy data was NOT recovered. UI should warn the user.
     */
    data class DestructiveFallback(
        val fromVersion: Int,
    ) : RestoreResult

    /**
     * Legacy file was missing or the copy step failed before Room was reopened.
     * The active database file was preserved (atomic-swap safety guarantee).
     */
    data class LegacyFileUnavailable(
        val reason: String,
    ) : RestoreResult

    /**
     * Anything else that went wrong. The active database file was preserved
     * via backup-restore on failure. The error is logged via Kermit.
     */
    data class Failure(
        val errorMessage: String,
        val throwableClassName: String,
    ) : RestoreResult
}
