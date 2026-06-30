package com.chriscartland.batterybutler.data.repository.auth

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.datanetwork.LabsAuthGateway
import com.chriscartland.batterybutler.datanetwork.auth.GoogleSignInBridge
import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.LabsProdGoogleOAuthClient
import com.chriscartland.batterybutler.domain.model.LabsStagingGoogleOAuthClient
import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.User
import com.chriscartland.batterybutler.domain.repository.LabsAuthRepository
import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import me.tatarka.inject.annotations.Inject

/**
 * Default [LabsAuthRepository]. Drives the Labs sign-in chain:
 *
 *   Google Sign-In (Labs OAuth client) --> Google ID token
 *     --> [LabsAuthGateway.signInToLabsWithGoogle] (signInWithIdp) --> Labs session
 *
 * The OAuth client is chosen by the currently-selected Labs network mode (staging vs prod). The
 * Google token's `aud` must be a Labs client the Labs Firebase project trusts, which is why this
 * uses [GoogleSignInBridge.signInWithClient] with the per-env client rather than the own-backend
 * [GoogleSignInBridge.signIn]. The Labs session lives in the singleton gateway (shared with the
 * sync calls); this repo just tracks UI state.
 */
@Inject
class DefaultLabsAuthRepository(
    private val googleSignInBridge: GoogleSignInBridge,
    private val labsAuthGateway: LabsAuthGateway,
    private val networkModeRepository: NetworkModeRepository,
    private val labsStagingOAuthClient: LabsStagingGoogleOAuthClient,
    private val labsProdOAuthClient: LabsProdGoogleOAuthClient,
) : LabsAuthRepository {
    private val log = Logger.withTag("DefaultLabsAuthRepository")

    private val _labsAuthState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val labsAuthState: StateFlow<AuthState> = _labsAuthState.asStateFlow()

    override suspend fun signInToLabs(): Result<User, AuthError> {
        val client = when (networkModeRepository.networkMode.first()) {
            is NetworkMode.LabsStaging -> {
                labsStagingOAuthClient.clientId to labsStagingOAuthClient.clientSecret
            }

            is NetworkMode.LabsProd -> {
                labsProdOAuthClient.clientId to labsProdOAuthClient.clientSecret
            }

            else -> {
                return fail(
                    AuthError.Configuration.NotConfigured(
                        message = "Not a Labs network mode",
                        cause = "Select Labs (staging) or Labs (prod) before signing in to Labs",
                    ),
                )
            }
        }
        val (clientId, clientSecret) = client
        if (clientId.isBlank()) {
            return fail(
                AuthError.Configuration.NotConfigured(
                    message = "Labs sign-in not configured",
                    cause = "No Labs OAuth client ID for this environment (owner setup pending)",
                ),
            )
        }

        _labsAuthState.value = AuthState.Authenticating
        return when (val signIn = googleSignInBridge.signInWithClient(clientId, clientSecret.ifBlank { null })) {
            is Result.Success -> {
                exchangeForLabsSession(signIn.data)
            }

            is Result.Error -> {
                log.w { "Labs Google Sign-In failed: ${signIn.error.message}" }
                fail(signIn.error)
            }
        }
    }

    private suspend fun exchangeForLabsSession(
        google: com.chriscartland.batterybutler.datanetwork.auth.GoogleIdToken,
    ): Result<User, AuthError> =
        when (val exchange = labsAuthGateway.signInToLabsWithGoogle(google.idToken)) {
            is Result.Success -> {
                val user = User(
                    id = google.email ?: google.idToken.take(USER_ID_FALLBACK_LEN),
                    email = google.email,
                    displayName = google.displayName,
                    photoUrl = google.photoUrl,
                )
                log.i { "Labs sign-in successful for ${google.email}" }
                _labsAuthState.value = AuthState.Authenticated(user)
                Result.Success(user)
            }

            is Result.Error -> {
                log.w { "Labs token exchange failed: ${exchange.error.message}" }
                fail(exchange.error)
            }
        }

    override suspend fun signOutLabs() {
        labsAuthGateway.signOutLabs()
        _labsAuthState.value = AuthState.Unauthenticated
    }

    override fun clearError() {
        if (_labsAuthState.value is AuthState.Failed) {
            _labsAuthState.value = AuthState.Unauthenticated
        }
    }

    private fun fail(error: AuthError): Result<User, AuthError> {
        _labsAuthState.value = AuthState.Failed(error)
        return Result.Error(error)
    }

    private companion object {
        const val USER_ID_FALLBACK_LEN = 32
    }
}
