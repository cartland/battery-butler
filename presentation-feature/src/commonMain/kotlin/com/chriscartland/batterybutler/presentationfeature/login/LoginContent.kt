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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.action_continue_no_sign_in
import com.chriscartland.batterybutler.composeresources.generated.resources.action_ok
import com.chriscartland.batterybutler.composeresources.generated.resources.action_try_again
import com.chriscartland.batterybutler.composeresources.generated.resources.app_name
import com.chriscartland.batterybutler.composeresources.generated.resources.auth_error_message_cancelled
import com.chriscartland.batterybutler.composeresources.generated.resources.auth_error_message_cant_connect
import com.chriscartland.batterybutler.composeresources.generated.resources.auth_error_message_coming_soon
import com.chriscartland.batterybutler.composeresources.generated.resources.auth_error_message_connection
import com.chriscartland.batterybutler.composeresources.generated.resources.auth_error_message_expired
import com.chriscartland.batterybutler.composeresources.generated.resources.auth_error_message_safe_data
import com.chriscartland.batterybutler.composeresources.generated.resources.auth_error_message_sign_in_again
import com.chriscartland.batterybutler.composeresources.generated.resources.auth_error_title_cancelled
import com.chriscartland.batterybutler.composeresources.generated.resources.auth_error_title_cant_connect
import com.chriscartland.batterybutler.composeresources.generated.resources.auth_error_title_coming_soon
import com.chriscartland.batterybutler.composeresources.generated.resources.auth_error_title_connection
import com.chriscartland.batterybutler.composeresources.generated.resources.auth_error_title_expired
import com.chriscartland.batterybutler.composeresources.generated.resources.auth_error_title_failed
import com.chriscartland.batterybutler.composeresources.generated.resources.auth_error_title_session
import com.chriscartland.batterybutler.composeresources.generated.resources.auth_error_title_unknown
import com.chriscartland.batterybutler.composeresources.generated.resources.login_action_sign_in_google
import com.chriscartland.batterybutler.composeresources.generated.resources.login_error_sign_in_unavailable
import com.chriscartland.batterybutler.composeresources.generated.resources.login_info_local_only
import com.chriscartland.batterybutler.composeresources.generated.resources.login_tagline
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_labs_sign_in
import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.Padding

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
 * @param isSignInAvailable Whether sign-in is configured.
 * @param isLabsMode Whether the selected backend is a Labs mode (labels the button "Sign in to Labs").
 * @param onGoogleSignIn Callback when user taps the sign-in button.
 * @param onSkipLogin Callback when user taps Skip button.
 * @param onDismissError Callback when user dismisses error dialog.
 */
@Composable
fun LoginContent(
    authState: AuthState,
    isSignInAvailable: Boolean,
    isLabsMode: Boolean,
    onGoogleSignIn: () -> Unit,
    onSkipLogin: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
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
                        isLabsMode = isLabsMode,
                        isLoading = true,
                        onGoogleSignIn = onGoogleSignIn,
                        onSkipLogin = onSkipLogin,
                    )
                }

                is AuthState.Unauthenticated -> {
                    LoginForm(
                        isSignInAvailable = isSignInAvailable,
                        isLabsMode = isLabsMode,
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
                        isLabsMode = isLabsMode,
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
}

@Composable
private fun LoginForm(
    isSignInAvailable: Boolean,
    isLabsMode: Boolean,
    isLoading: Boolean,
    onGoogleSignIn: () -> Unit,
    onSkipLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Padding.extraLarge),
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
            text = composeStringResource(Res.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tagline
        Text(
            text = composeStringResource(Res.string.login_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Prominent primary sign-in button. Labelled for the selected backend: "Sign in to Labs"
        // in a Labs mode, otherwise "Sign in with Google".
        if (isSignInAvailable) {
            Button(
                onClick = onGoogleSignIn,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = MaterialTheme.shapes.medium,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = composeStringResource(
                            if (isLabsMode) {
                                Res.string.settings_labs_sign_in
                            } else {
                                Res.string.login_action_sign_in_google
                            },
                        ),
                    )
                }
            }
        } else {
            // Sign-in not configured - show "Coming Soon" message
            Text(
                text = composeStringResource(Res.string.login_error_sign_in_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(Padding.standard),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Skip button - always available for local-only usage
        OutlinedButton(
            onClick = onSkipLogin,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(composeStringResource(Res.string.action_continue_no_sign_in))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Info text about local-only usage
        Text(
            text = composeStringResource(Res.string.login_info_local_only),
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
                    Text(composeStringResource(Res.string.action_try_again))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(composeStringResource(Res.string.action_ok))
            }
        },
    )
}

/**
 * Returns user-friendly (title, message) pair for the given error.
 */
@Composable
private fun getErrorText(error: AuthError): Pair<String, String> =
    when (error) {
        is AuthError.Configuration.NotConfigured -> Pair(
            composeStringResource(Res.string.auth_error_title_coming_soon),
            composeStringResource(Res.string.auth_error_message_coming_soon),
        )

        is AuthError.Configuration.ServerUnavailable -> Pair(
            composeStringResource(Res.string.auth_error_title_cant_connect),
            composeStringResource(Res.string.auth_error_message_cant_connect),
        )

        is AuthError.SignIn.Cancelled -> Pair(
            composeStringResource(Res.string.auth_error_title_cancelled),
            composeStringResource(Res.string.auth_error_message_cancelled),
        )

        is AuthError.SignIn.NetworkError -> Pair(
            composeStringResource(Res.string.auth_error_title_connection),
            composeStringResource(Res.string.auth_error_message_connection),
        )

        is AuthError.SignIn.Failed -> Pair(
            composeStringResource(Res.string.auth_error_title_failed),
            error.cause ?: composeStringResource(Res.string.auth_error_message_safe_data),
        )

        is AuthError.Token.Invalid -> Pair(
            composeStringResource(Res.string.auth_error_title_session),
            composeStringResource(Res.string.auth_error_message_sign_in_again),
        )

        is AuthError.Token.Expired -> Pair(
            composeStringResource(Res.string.auth_error_title_expired),
            composeStringResource(Res.string.auth_error_message_expired),
        )

        is AuthError.Unknown -> Pair(
            composeStringResource(Res.string.auth_error_title_unknown),
            composeStringResource(Res.string.auth_error_message_safe_data),
        )
    }

@Preview(showBackground = true)
@Composable
fun LoginContentLabsPreview() {
    BatteryButlerTheme {
        LoginContent(
            authState = AuthState.Unauthenticated(),
            isSignInAvailable = true,
            isLabsMode = true,
            onGoogleSignIn = {},
            onSkipLogin = {},
            onDismissError = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginContentUnauthenticatedPreview() {
    BatteryButlerTheme {
        LoginContent(
            authState = AuthState.Unauthenticated(),
            isSignInAvailable = true,
            isLabsMode = false,
            onGoogleSignIn = {},
            onSkipLogin = {},
            onDismissError = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginContentAuthenticatingPreview() {
    BatteryButlerTheme {
        LoginContent(
            authState = AuthState.Authenticating,
            isSignInAvailable = true,
            isLabsMode = false,
            onGoogleSignIn = {},
            onSkipLogin = {},
            onDismissError = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginContentNotConfiguredPreview() {
    BatteryButlerTheme {
        LoginContent(
            authState = AuthState.Unauthenticated(),
            isSignInAvailable = false,
            isLabsMode = false,
            onGoogleSignIn = {},
            onSkipLogin = {},
            onDismissError = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginContentErrorPreview() {
    BatteryButlerTheme {
        LoginContent(
            authState = AuthState.Failed(
                AuthError.SignIn.NetworkError(
                    message = "Network error",
                    cause = "Unable to connect to server",
                ),
            ),
            isSignInAvailable = true,
            isLabsMode = false,
            onGoogleSignIn = {},
            onSkipLogin = {},
            onDismissError = {},
        )
    }
}
