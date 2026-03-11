package com.chriscartland.batterybutler.domain.model

/**
 * Typed error hierarchy for data operations.
 *
 * Each sealed interface represents a category of errors from a specific layer:
 * - [Network] - Errors from remote data sources (gRPC, HTTP)
 * - [Database] - Errors from local persistence (Room, SQLite)
 * - [Ai] - Errors from AI/ML operations
 * - [Unknown] - Fallback for unexpected errors
 *
 * Implements [AppError] to enable use with generic [Result] type.
 */
sealed interface DataError : AppError {
    override val message: String
    override val cause: String?

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
