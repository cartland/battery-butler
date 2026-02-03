package com.chriscartland.batterybutler.domain.model

/**
 * Sealed interface representing the result of a data operation.
 *
 * Project code NEVER throws exceptions except CancellationException.
 * Library exceptions are caught at data layer boundaries and returned as [Error].
 */
sealed interface DataResult<out T> {
    data class Success<T>(
        val data: T,
    ) : DataResult<T>

    data class Error(
        val error: DataError,
    ) : DataResult<Nothing>
}

/**
 * Typed error hierarchy for data operations.
 *
 * Each sealed interface represents a category of errors from a specific layer:
 * - [Network] - Errors from remote data sources (gRPC, HTTP)
 * - [Database] - Errors from local persistence (Room, SQLite)
 * - [Ai] - Errors from AI/ML operations
 * - [Unknown] - Fallback for unexpected errors
 */
sealed interface DataError {
    val message: String
    val cause: String?

    sealed interface Network : DataError {
        data class ConnectionFailed(
            override val message: String = "Unable to connect",
            override val cause: String? = null,
        ) : Network

        data class Timeout(
            override val message: String = "Request timed out",
            override val cause: String? = null,
        ) : Network

        data class ServerError(
            override val message: String = "Server error",
            override val cause: String? = null,
        ) : Network

        data class NotReady(
            override val message: String = "Network not ready",
            override val cause: String? = null,
        ) : Network

        data class PushFailed(
            override val message: String = "Push failed",
            override val cause: String? = null,
        ) : Network
    }

    sealed interface Database : DataError {
        data class ReadFailed(
            override val message: String = "Read failed",
            override val cause: String? = null,
        ) : Database

        data class WriteFailed(
            override val message: String = "Write failed",
            override val cause: String? = null,
        ) : Database

        data class ConstraintViolation(
            override val message: String = "Constraint violation",
            override val cause: String? = null,
        ) : Database
    }

    sealed interface Ai : DataError {
        data class ApiError(
            override val message: String = "AI API error",
            override val cause: String? = null,
        ) : Ai

        data class ParsingError(
            override val message: String = "Parse failed",
            override val cause: String? = null,
        ) : Ai
    }

    data class Unknown(
        override val message: String = "Unknown error",
        override val cause: String? = null,
    ) : DataError
}

/**
 * Extension function to map successful results.
 */
inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> =
    when (this) {
        is DataResult.Success -> DataResult.Success(transform(data))
        is DataResult.Error -> this
    }

/**
 * Extension function to get data or null.
 */
fun <T> DataResult<T>.getOrNull(): T? =
    when (this) {
        is DataResult.Success -> data
        is DataResult.Error -> null
    }

/**
 * Extension function to get data or throw.
 */
fun <T> DataResult<T>.getOrThrow(): T =
    when (this) {
        is DataResult.Success -> data
        is DataResult.Error -> throw IllegalStateException(error.message)
    }
