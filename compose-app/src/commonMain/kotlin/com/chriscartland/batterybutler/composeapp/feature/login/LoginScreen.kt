package com.chriscartland.batterybutler.composeapp.feature.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.presentationfeature.login.LoginContent
import com.chriscartland.batterybutler.viewmodel.login.LoginViewModel

/**
 * Login screen that handles authentication flow.
 *
 * @param viewModel The LoginViewModel managing auth state.
 * @param onLoginSuccess Callback when user successfully signs in.
 * @param onSkipLogin Callback when user chooses to continue without signing in.
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onSkipLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    // Navigate on successful authentication
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onLoginSuccess()
        }
    }

    LoginContent(
        authState = authState,
        isSignInAvailable = viewModel.isSignInAvailable,
        onGoogleSignIn = viewModel::signInWithGoogle,
        onSkipLogin = onSkipLogin,
        onDismissError = viewModel::dismissError,
        modifier = modifier,
    )
}
