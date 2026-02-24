package com.chriscartland.batterybutler.domain.model

/**
 * Represents the status and outcome of an asynchronous bulk operation.
 * Used for long-running domain processes such as syncing, importing, or deleting records
 * to provide granular feedback.
 */
sealed interface BatchOperationResult {
    /**
     * An intermediate status update emitted while the batch operation is ongoing.
     * @property message A human-readable progress string (e.g., "Importing 5 of 10...").
     */
    data class Progress(
        val message: String,
    ) : BatchOperationResult

    /**
     * Represents the successful completion of the batch operation.
     * @property message A human-readable completion string (e.g., "10 items imported successfully").
     */
    data class Success(
        val message: String,
    ) : BatchOperationResult

    /**
     * Represents a failure during the batch operation.
     * @property error The specific [DataError] describing what caused the batch operation to fail.
     */
    data class Error(
        val error: DataError,
    ) : BatchOperationResult
}
