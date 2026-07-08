package com.chriscartland.batterybutler.datanetwork

import com.chriscartland.batterybutler.datanetwork.rest.FirebaseIdTokenProvider
import com.chriscartland.batterybutler.datanetwork.rest.createSyncHttpClient
import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.LabsFirebaseApiKey
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.repository.DataModeRepository
import kotlinx.coroutines.flow.first
import me.tatarka.inject.annotations.Inject

/**
 * The Labs Firebase **Web API key** for [mode]'s project: the prod key in a prod Labs mode, else the
 * staging key. `signInWithIdp` mints a Firebase ID token from whichever project owns the key, so the
 * key must match the env's backend (a staging token is rejected by the prod backend and vice versa).
 * Blank when that env is unconfigured.
 */
fun apiKeyForMode(
    mode: DataMode,
    keys: LabsFirebaseApiKey,
): String =
    when (mode) {
        is DataMode.LabsProd -> keys.prod
        else -> keys.staging
    }

/**
 * Default [LabsAuthGateway] — owns the per-env [FirebaseIdTokenProvider] session(s).
 *
 * Provided as a DI singleton (see AppComponent) so a session is shared between the sign-in trigger
 * and [DelegatingRemoteDataSource]'s token reads. The Firebase Web API key is selected by the
 * **current [DataMode]** (staging vs prod are separate Firebase projects — see [apiKeyForMode]),
 * and one provider is kept per env key so a mode switch uses that env's session. The HTTP client +
 * providers are built lazily, so nothing is created until a Labs mode is actually used. Keys are
 * blank until owner setup; the provider then reports `NotConfigured` and yields no token, so the app
 * degrades (sends no Bearer header → 401) rather than crashing.
 */
@Inject
class DefaultLabsAuthGateway(
    private val dataModeRepository: DataModeRepository,
    private val labsFirebaseApiKey: LabsFirebaseApiKey,
) : LabsAuthGateway {
    private val httpClient by lazy { createSyncHttpClient() }

    // One session per env Firebase key. Keyed by apiKey so staging and prod each keep their own
    // in-memory session across mode switches; created on first use of that env.
    private val providersByApiKey = mutableMapOf<String, FirebaseIdTokenProvider>()

    private suspend fun currentProvider(): FirebaseIdTokenProvider {
        val apiKey = apiKeyForMode(dataModeRepository.dataMode.first(), labsFirebaseApiKey)
        return providersByApiKey.getOrPut(apiKey) {
            FirebaseIdTokenProvider(httpClient = httpClient, apiKey = apiKey)
        }
    }

    override suspend fun signInToLabsWithGoogle(googleIdToken: String): Result<Unit, AuthError> = currentProvider().signInWithGoogle(googleIdToken)

    override suspend fun getLabsIdToken(): String? = currentProvider().getIdToken()

    override suspend fun signOutLabs() = currentProvider().signOut()
}
