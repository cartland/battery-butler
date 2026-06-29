package com.chriscartland.batterybutler.datanetwork.rest

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Supplies a Labs **Firebase ID token** for the `Authorization: Bearer` header on the Labs
 * `/v1/battery-butler/sync` endpoint, via the Firebase Auth REST IdP flow (proven by the
 * Workstream D spike):
 *
 * ```
 * Google ID token --accounts:signInWithIdp--> Labs ID token + refresh token   (interactive, once)
 *   refresh token --securetoken /v1/token--> fresh Labs ID token              (non-interactive)
 * ```
 *
 * [signInWithGoogle] is the one interactive step (it needs a Google ID token whose audience the
 * Labs Firebase project trusts — the OAuth-client owner setup). Afterwards [getIdToken] refreshes
 * silently with the stored refresh token. The HTTP boundary is wrapped (project rule: never throw
 * except [CancellationException]); a failure returns null/[Result.Error] so an unconfigured or
 * signed-out app sends no header and gets a 401 — it degrades, never crashes.
 *
 * Stateful + shared across sync calls, so construct once (one instance per app). [getIdToken] is
 * the `tokenProvider` passed to [RestRemoteDataSource].
 *
 * @param apiKey the Labs Firebase **Web API key** (Workstream E injects it; blank = unconfigured).
 * @param now epoch-millis source, injectable for tests.
 */
@OptIn(ExperimentalTime::class)
internal class FirebaseIdTokenProvider(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private data class Session(
        val idToken: String,
        val refreshToken: String,
        val expiresAtMs: Long,
    )

    private val log = Logger.withTag("FirebaseIdTokenProvider")
    private val mutex = Mutex()
    private var session: Session? = null

    /**
     * Exchange a freshly-obtained Google ID token for a Labs session. Interactive — call once after
     * Google Sign-In; [getIdToken] keeps it fresh afterwards.
     */
    suspend fun signInWithGoogle(googleIdToken: String): Result<Unit, AuthError> {
        if (apiKey.isBlank()) {
            return Result.Error(AuthError.Configuration.NotConfigured())
        }
        return try {
            val response = httpClient.post(SIGN_IN_WITH_IDP_URL) {
                parameter("key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(
                    SignInWithIdpRequest(postBody = "id_token=$googleIdToken&providerId=google.com"),
                )
            }
            if (!response.status.isSuccess()) {
                return Result.Error(
                    AuthError.SignIn.Failed(cause = "signInWithIdp HTTP ${response.status.value}"),
                )
            }
            val body = response.body<SignInWithIdpResponse>()
            if (body.idToken.isBlank() || body.refreshToken.isBlank()) {
                return Result.Error(AuthError.SignIn.Failed(cause = "signInWithIdp returned no token"))
            }
            mutex.withLock { session = body.toSession() }
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(AuthError.SignIn.NetworkError(cause = e.message))
        }
    }

    /**
     * The current Labs Firebase ID token, refreshing if it is within [EXPIRY_BUFFER_MS] of expiry;
     * null if not signed in or the refresh fails (caller sends no Authorization header).
     */
    suspend fun getIdToken(): String? =
        mutex.withLock {
            val current = session ?: return null
            if (now() < current.expiresAtMs - EXPIRY_BUFFER_MS) {
                return current.idToken
            }
            val refreshed = refresh(current.refreshToken) ?: return null
            session = refreshed
            refreshed.idToken
        }

    /** Clear the session (e.g. on sign-out). */
    suspend fun signOut() = mutex.withLock { session = null }

    private suspend fun refresh(refreshToken: String): Session? {
        if (apiKey.isBlank()) return null
        return try {
            val response = httpClient.post(SECURE_TOKEN_URL) {
                parameter("key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(RefreshTokenRequest(refreshToken = refreshToken))
            }
            if (!response.status.isSuccess()) return null
            val body = response.body<RefreshTokenResponse>()
            if (body.idToken.isBlank()) {
                null
            } else {
                Session(
                    idToken = body.idToken,
                    refreshToken = body.refreshToken.ifBlank { refreshToken },
                    expiresAtMs = expiresAtFrom(body.expiresIn),
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.w(e) { "Labs Firebase token refresh failed" }
            null
        }
    }

    private fun SignInWithIdpResponse.toSession() = Session(idToken = idToken, refreshToken = refreshToken, expiresAtMs = expiresAtFrom(expiresIn))

    private fun expiresAtFrom(expiresInSeconds: String): Long = now() + (expiresInSeconds.toLongOrNull() ?: 0L) * MILLIS_PER_SECOND

    private companion object {
        const val SIGN_IN_WITH_IDP_URL =
            "https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp"
        const val SECURE_TOKEN_URL = "https://securetoken.googleapis.com/v1/token"
        const val EXPIRY_BUFFER_MS = 60_000L
        const val MILLIS_PER_SECOND = 1_000L
    }
}
