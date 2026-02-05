package com.chriscartland.batterybutler.domain.model

/**
 * Represents the current authentication state of the application.
 *
 * State transitions:
 * - App starts in [Unknown] while checking for stored credentials
 * - Transitions to [Unauthenticated] if no valid credentials found
 * - Transitions to [Authenticating] when sign-in is initiated
 * - Transitions to [Authenticated] on successful sign-in
 * - Transitions to [Failed] if sign-in fails
 * - [Failed] can transition back to [Unauthenticated] when error is cleared
 */
sealed interface AuthState {
    /**
     * Initial state when the app is checking for stored credentials.
     * UI should show a loading indicator or splash screen.
     */
    data object Unknown : AuthState

    /**
     * No user is signed in.
     * UI should show sign-in options.
     */
    data object Unauthenticated : AuthState

    /**
     * Sign-in is in progress.
     * UI should show a loading indicator and disable sign-in buttons.
     */
    data object Authenticating : AuthState

    /**
     * User is signed in.
     * @property user The authenticated user.
     */
    data class Authenticated(
        val user: User,
    ) : AuthState

    /**
     * Authentication failed with an error.
     * UI should show the error message with option to retry or dismiss.
     * @property error The authentication error that occurred.
     */
    data class Failed(
        val error: AuthError,
    ) : AuthState
}
