package com.chriscartland.batterybutler.presentationfeature.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme

/**
 * Login screen content.
 *
 * Shows different UI based on [authState]:
 * - [AuthState.Unknown]: Loading indicator
 * - [AuthState.Unauthenticated]: Sign-in options
 * - [AuthState.Authenticating]: Loading with disabled buttons
 * - [AuthState.Failed]: Error dialog
 *
 * @param authState Current authentication state.
 * @param isSignInAvailable Whether Google Sign-In is configured.
 * @param onGoogleSignIn Callback when user taps Google Sign-In button.
 * @param onSkipLogin Callback when user taps Skip button.
 * @param onDismissError Callback when user dismisses error dialog.
 */
@Composable
fun LoginContent(
    authState: AuthState,
    isSignInAvailable: Boolean,
    onGoogleSignIn: () -> Unit,
    onSkipLogin: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when (authState) {
            is AuthState.Unknown -> {
                // Loading while checking stored credentials
                CircularProgressIndicator()
            }
            is AuthState.Authenticating -> {
                // Sign-in in progress
                LoginForm(
                    isSignInAvailable = isSignInAvailable,
                    isLoading = true,
                    onGoogleSignIn = onGoogleSignIn,
                    onSkipLogin = onSkipLogin,
                )
            }
            is AuthState.Unauthenticated -> {
                LoginForm(
                    isSignInAvailable = isSignInAvailable,
                    isLoading = false,
                    onGoogleSignIn = onGoogleSignIn,
                    onSkipLogin = onSkipLogin,
                )
            }
            is AuthState.Authenticated -> {
                // This state is handled by navigation (navigate to main screen)
                // Show nothing or a brief loading indicator
                CircularProgressIndicator()
            }
            is AuthState.Failed -> {
                // Show login form with error dialog
                LoginForm(
                    isSignInAvailable = isSignInAvailable,
                    isLoading = false,
                    onGoogleSignIn = onGoogleSignIn,
                    onSkipLogin = onSkipLogin,
                )
                ErrorDialog(
                    error = authState.error,
                    onDismiss = onDismissError,
                    onRetry = onGoogleSignIn,
                )
            }
        }
    }
}

@Composable
private fun LoginForm(
    isSignInAvailable: Boolean,
    isLoading: Boolean,
    onGoogleSignIn: () -> Unit,
    onSkipLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // App logo/icon
        Icon(
            imageVector = Icons.Default.BatteryChargingFull,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // App title
        Text(
            text = "Battery Butler",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tagline
        Text(
            text = "Track your battery replacements",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Google Sign-In button
        if (isSignInAvailable) {
            Button(
                onClick = onGoogleSignIn,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Sign in with Google")
                }
            }
        } else {
            // Sign-in not configured - show "Coming Soon" message
            Text(
                text = "Sign-in will be available in a future update",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Skip button - always available for local-only usage
        OutlinedButton(
            onClick = onSkipLogin,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Continue without signing in")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Info text about local-only usage
        Text(
            text = "Your data stays on this device. Sign in to sync across devices.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ErrorDialog(
    error: AuthError,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    val (title, message) = getErrorText(error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            if (error !is AuthError.Configuration.NotConfigured) {
                TextButton(onClick = {
                    onDismiss()
                    onRetry()
                }) {
                    Text("Try Again")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
    )
}

/**
 * Returns user-friendly (title, message) pair for the given error.
 */
private fun getErrorText(error: AuthError): Pair<String, String> =
    when (error) {
        is AuthError.Configuration.NotConfigured -> Pair(
            "Coming Soon",
            "Sign-in will be available in a future update. Continue using the app locally.",
        )
        is AuthError.Configuration.ServerUnavailable -> Pair(
            "Can't Connect",
            "Unable to reach the server. Your data is safe on this device.",
        )
        is AuthError.SignIn.Cancelled -> Pair(
            "Sign In Cancelled",
            "No problem! You can sign in anytime from Settings.",
        )
        is AuthError.SignIn.NetworkError -> Pair(
            "Connection Problem",
            "Check your internet connection and try again.",
        )
        is AuthError.SignIn.Failed -> Pair(
            "Sign In Failed",
            error.cause ?: "Please try again. Your local data is safe.",
        )
        is AuthError.Token.Invalid -> Pair(
            "Session Error",
            "Please sign in again to continue.",
        )
        is AuthError.Token.Expired -> Pair(
            "Session Expired",
            "Please sign in again to sync your data.",
        )
        is AuthError.Unknown -> Pair(
            "Something Went Wrong",
            "Please try again. Your local data is safe.",
        )
    }

@Preview(showBackground = true)
@Composable
private fun LoginContentUnauthenticatedPreview() {
    BatteryButlerTheme {
        LoginContent(
            authState = AuthState.Unauthenticated,
            isSignInAvailable = true,
            onGoogleSignIn = {},
            onSkipLogin = {},
            onDismissError = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginContentAuthenticatingPreview() {
    BatteryButlerTheme {
        LoginContent(
            authState = AuthState.Authenticating,
            isSignInAvailable = true,
            onGoogleSignIn = {},
            onSkipLogin = {},
            onDismissError = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginContentNotConfiguredPreview() {
    BatteryButlerTheme {
        LoginContent(
            authState = AuthState.Unauthenticated,
            isSignInAvailable = false,
            onGoogleSignIn = {},
            onSkipLogin = {},
            onDismissError = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginContentErrorPreview() {
    BatteryButlerTheme {
        LoginContent(
            authState = AuthState.Failed(
                AuthError.SignIn.NetworkError(
                    message = "Network error",
                    cause = "Unable to connect to server",
                ),
            ),
            isSignInAvailable = true,
            onGoogleSignIn = {},
            onSkipLogin = {},
            onDismissError = {},
        )
    }
}
