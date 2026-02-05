package com.chriscartland.batterybutler.domain.model

/**
 * Sealed interface representing authentication-related errors.
 *
 * Organized into categories:
 * - [Configuration] - System setup issues (server unavailable, OAuth not configured)
 * - [SignIn] - Errors during sign-in flow (cancelled, failed, network)
 * - [Token] - Token-related errors (invalid, expired)
 * - [Unknown] - Unexpected errors
 *
 * Implements [AppError] to enable use with generic [Result] type.
 */
sealed interface AuthError : AppError {
    override val message: String
    override val cause: String?

    /**
     * Configuration-related errors that prevent authentication from working.
     * These indicate system setup issues rather than user actions.
     */
    sealed interface Configuration : AuthError {
        /**
         * OAuth credentials are not configured for this build.
         */
        data class NotConfigured(
            override val message: String = "Sign-in not available",
            override val cause: String? = null,
        ) : Configuration

        /**
         * The authentication server is not reachable.
         */
        data class ServerUnavailable(
            override val message: String = "Server unavailable",
            override val cause: String? = null,
        ) : Configuration
    }

    /**
     * Errors that occur during the sign-in flow.
     */
    sealed interface SignIn : AuthError {
        /**
         * User cancelled the sign-in flow.
         */
        data class Cancelled(
            override val message: String = "Sign-in cancelled",
            override val cause: String? = null,
        ) : SignIn

        /**
         * Sign-in failed for an unspecified reason.
         */
        data class Failed(
            override val message: String = "Sign-in failed",
            override val cause: String? = null,
        ) : SignIn

        /**
         * Network error during sign-in.
         */
        data class NetworkError(
            override val message: String = "Network error",
            override val cause: String? = null,
        ) : SignIn
    }

    /**
     * Token-related errors.
     */
    sealed interface Token : AuthError {
        /**
         * The token is invalid or malformed.
         */
        data class Invalid(
            override val message: String = "Invalid token",
            override val cause: String? = null,
        ) : Token

        /**
         * The token has expired.
         */
        data class Expired(
            override val message: String = "Session expired",
            override val cause: String? = null,
        ) : Token
    }

    /**
     * Fallback for unexpected authentication errors.
     */
    data class Unknown(
        override val message: String = "Unknown error",
        override val cause: String? = null,
    ) : AuthError
}
