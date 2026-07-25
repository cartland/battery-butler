package com.chriscartland.batterybutler.data.repository.auth

import com.chriscartland.batterybutler.datanetwork.LabsSessionInvalidation
import com.chriscartland.batterybutler.datanetwork.auth.GoogleSignInBridge
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.LabsFirebaseApiKey
import com.chriscartland.batterybutler.domain.model.LabsProdGoogleOAuthClient
import com.chriscartland.batterybutler.domain.model.LabsSessionRestoreResult
import com.chriscartland.batterybutler.domain.model.LabsStagingGoogleOAuthClient
import com.chriscartland.batterybutler.domain.model.SignedOutCause
import com.chriscartland.batterybutler.domain.model.SyncAuthReason
import com.chriscartland.batterybutler.domain.model.User
import com.chriscartland.batterybutler.testcommon.FakeDataModeRepository
import com.chriscartland.batterybutler.testcommon.FakeLabsAuthGateway
import com.chriscartland.batterybutler.testcommon.FakeLabsSessionStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * The repository half of reactive session loss: when the gateway reports an environment's
 * session authoritatively rejected (terminal 401 after the retry-once policy, or a rejected
 * refresh), the repository must flip that environment's [AuthState] to
 * [AuthState.Unauthenticated] with [SignedOutCause.SESSION_EXPIRED], clear the believed-signed-in
 * user, and resolve the cold-start gate as [LabsSessionRestoreResult.INVALID]. Local device data
 * is structurally untouchable from here (no repository dependency on it) — the sync-manager test
 * asserts the rows survive.
 *
 * Lives in jvmTest (not commonTest) because constructing [DefaultLabsAuthRepository] needs the
 * platform `GoogleSignInBridge` actual, which common code cannot instantiate.
 */
class LabsSessionLossReactionTest {
    @Test
    fun `a session invalidation flips the env to session-expired and clears the believed user`() =
        runTest {
            val stagingKey = "staging-key"
            val gateway = FakeLabsAuthGateway()
            val sessionStorage = FakeLabsSessionStorage()
            sessionStorage.saveUser(
                stagingKey,
                User(id = "user-1", email = "user@example.com", displayName = "User", photoUrl = null),
            )
            val repository = DefaultLabsAuthRepository(
                googleSignInBridge = GoogleSignInBridge(),
                labsAuthGateway = gateway,
                dataModeRepository = FakeDataModeRepository(DataMode.LabsStaging(url = "https://staging.example")),
                labsFirebaseApiKey = LabsFirebaseApiKey(staging = stagingKey, prod = "prod-key"),
                labsStagingOAuthClient = LabsStagingGoogleOAuthClient(clientId = "labs-client", clientSecret = ""),
                labsProdOAuthClient = LabsProdGoogleOAuthClient(clientId = "", clientSecret = ""),
                labsSessionStorage = sessionStorage,
                scope = backgroundScope,
            )

            // The believed-signed-in user resolves to Authenticated and triggers the restore.
            runCurrent()
            assertIs<AuthState.Authenticated>(repository.labsAuthState.first())
            assertEquals(1, gateway.restoreCount, "a believed-signed-in Labs cold start restores the session")

            // The gateway reports the session authoritatively rejected (it has already cleared
            // its in-memory session + the persisted refresh token by the time this event fires).
            gateway.emitInvalidation(
                LabsSessionInvalidation(environmentKey = stagingKey, reason = SyncAuthReason.TOKEN_INVALID),
            )
            runCurrent()

            val state = repository.labsAuthState.first()
            assertIs<AuthState.Unauthenticated>(state, "the env must flip to signed out, got $state")
            assertEquals(SignedOutCause.SESSION_EXPIRED, state.cause, "a reactive loss carries the session-expired cause")
            assertNull(sessionStorage.observeUser(stagingKey).first(), "the believed-signed-in user must be cleared")
            assertEquals(
                LabsSessionRestoreResult.INVALID,
                repository.awaitLabsSessionRestore(),
                "the cold-start gate resolves as INVALID, so sync proceeds and reports sign-in required",
            )
        }
}
