package com.chriscartland.batterybutler.datanetwork

import com.chriscartland.batterybutler.datanetwork.rest.FirebaseIdTokenProvider
import com.chriscartland.batterybutler.datanetwork.rest.createSyncHttpClient
import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.LabsFirebaseApiKey
import com.chriscartland.batterybutler.domain.model.Result
import me.tatarka.inject.annotations.Inject

/**
 * Default [LabsAuthGateway] — owns the single [FirebaseIdTokenProvider] session.
 *
 * Provided as a DI singleton (see AppComponent) so the session is shared between the sign-in
 * trigger and [DelegatingRemoteDataSource]'s token reads. The HTTP client + provider are built
 * lazily, so nothing is created until a Labs mode is actually used. [LabsFirebaseApiKey] is blank
 * until owner setup; the provider then reports `NotConfigured` and yields no token, so the app
 * degrades (sends no Bearer header → 401) rather than crashing.
 */
@Inject
class DefaultLabsAuthGateway(
    private val labsFirebaseApiKey: LabsFirebaseApiKey,
) : LabsAuthGateway {
    private val httpClient by lazy { createSyncHttpClient() }
    private val provider by lazy {
        FirebaseIdTokenProvider(httpClient = httpClient, apiKey = labsFirebaseApiKey.apiKey)
    }

    override suspend fun signInToLabsWithGoogle(googleIdToken: String): Result<Unit, AuthError> = provider.signInWithGoogle(googleIdToken)

    override suspend fun getLabsIdToken(): String? = provider.getIdToken()

    override suspend fun signOutLabs() = provider.signOut()
}
