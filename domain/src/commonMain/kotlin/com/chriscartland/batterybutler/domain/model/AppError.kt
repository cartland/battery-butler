package com.chriscartland.batterybutler.domain.model

/**
 * Base interface for all application errors.
 *
 * Enables generic Result<D, E : AppError> pattern where callers can constrain
 * the error type to specific subsets of the error hierarchy.
 *
 * All error types in the application should implement this interface:
 * - [DataError] - Errors from data layer operations (network, database, AI)
 * - Custom error types for domain-specific failures
 *
 * @property message Human-readable error description suitable for logging or display.
 * @property cause Optional underlying cause, typically from a caught exception.
 */
interface AppError {
    val message: String
    val cause: String?
}
