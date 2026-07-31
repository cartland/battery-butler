package com.chriscartland.batterybutler.datanetwork.rest

import com.chriscartland.batterybutler.datanetwork.LabsSignInIdentity
import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.Result
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseIdTokenProviderTest {
    @Test
    fun `signInWithGoogle exchanges the Google token and getIdToken returns the Labs id token`() =
        runTest {
            var seenKey: String? = null
            var seenPath: String? = null
            var seenBody: String? = null
            val provider = provider(backgroundScope) { request ->
                seenKey = request.url.parameters["key"]
                seenPath = request.url.encodedPath
                seenBody = (request.body as? io.ktor.http.content.TextContent)?.text
                respondJson(SIGN_IN_JSON)
            }

            val result = provider.signInWithGoogle("google-tok")

            assertIs<Result.Success<LabsSignInIdentity>>(result)
            assertEquals("test-api-key", seenKey)
            assertTrue(seenPath?.endsWith("accounts:signInWithIdp") == true)
            assertTrue(seenBody?.contains("id_token=google-tok&providerId=google.com") == true)
            assertEquals("labs-id-1", provider.getIdToken())
        }

    /**
     * `signInWithIdp`'s `localId` is the **Firebase uid** — the id the Labs backend authorizes
     * and attributes writes with (e.g. device-image `uploadedByUid`). The sign-in result must
     * surface it (plus the account email) so the auth repository can key [com.chriscartland
     * .batterybutler.domain.model.User.id] on it instead of the Google profile email.
     */
    @Test
    fun `signInWithGoogle returns the Firebase uid and email minted by signInWithIdp`() =
        runTest {
            val provider = provider(backgroundScope) { respondJson(SIGN_IN_JSON) }

            val result = provider.signInWithGoogle("google-tok")

            assertIs<Result.Success<LabsSignInIdentity>>(result)
            assertEquals(
                LabsSignInIdentity(firebaseUid = "uid1", email = "a@example.com"),
                result.data,
            )
        }

    @Test
    fun `signInWithGoogle treats a blank localId and email as absent`() =
        runTest {
            val provider = provider(backgroundScope) {
                // Lenient wire boundary: every response field defaults, so a response missing
                // localId/email parses as blanks — those must surface as null, not "".
                respondJson("""{"idToken":"labs-id-1","refreshToken":"refresh-1","expiresIn":"3600"}""")
            }

            val result = provider.signInWithGoogle("google-tok")

            assertIs<Result.Success<LabsSignInIdentity>>(result)
            assertEquals(LabsSignInIdentity(firebaseUid = null, email = null), result.data)
        }

    @Test
    fun `getIdToken is null before sign-in`() =
        runTest {
            val provider = provider(backgroundScope) { respondJson(SIGN_IN_JSON) }
            assertNull(provider.getIdToken())
        }

    @Test
    fun `getIdToken refreshes via securetoken when the id token is near expiry`() =
        runTest {
            var nowMs = 1_000_000L
            var refreshCalls = 0
            val provider = provider(backgroundScope, now = { nowMs }) { request ->
                if (request.url.host.contains("securetoken")) {
                    refreshCalls++
                    respondJson(REFRESH_JSON)
                } else {
                    respondJson(SIGN_IN_JSON) // expiresIn 3600 -> expiresAt = now + 3_600_000
                }
            }

            assertIs<Result.Success<*>>(provider.signInWithGoogle("google-tok"))
            assertEquals("labs-id-1", provider.getIdToken()) // fresh, no refresh
            assertEquals(0, refreshCalls)

            nowMs += 3_600_000L // past expiry (minus the 5-minute buffer)
            assertEquals("labs-id-2", provider.getIdToken()) // refreshed
            assertEquals(1, refreshCalls)
        }

    @Test
    fun `signInWithGoogle with a blank api key reports NotConfigured and makes no call`() =
        runTest {
            var called = false
            val provider = provider(backgroundScope, apiKey = "") {
                called = true
                respondJson(SIGN_IN_JSON)
            }

            val result = provider.signInWithGoogle("google-tok")

            assertIs<Result.Error<AuthError>>(result)
            assertIs<AuthError.Configuration.NotConfigured>(result.error)
            assertTrue(!called)
        }

    @Test
    fun `signInWithGoogle maps an HTTP error to SignIn Failed`() =
        runTest {
            val provider = provider(backgroundScope) {
                respond(
                    content = """{"error":{"message":"INVALID_IDP_RESPONSE"}}""",
                    status = HttpStatusCode.BadRequest,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }

            val result = provider.signInWithGoogle("google-tok")

            assertIs<Result.Error<AuthError>>(result)
            assertIs<AuthError.SignIn.Failed>(result.error)
            assertNull(provider.getIdToken())
        }

    @Test
    fun `signInWithGoogle maps a network exception to SignIn NetworkError`() =
        runTest {
            val provider = provider(backgroundScope) { throw RuntimeException("boom") }

            val result = provider.signInWithGoogle("google-tok")

            assertIs<Result.Error<AuthError>>(result)
            assertIs<AuthError.SignIn.NetworkError>(result.error)
        }

    @Test
    fun `signOut clears the session`() =
        runTest {
            val provider = provider(backgroundScope) { respondJson(SIGN_IN_JSON) }
            assertIs<Result.Success<*>>(provider.signInWithGoogle("google-tok"))
            assertEquals("labs-id-1", provider.getIdToken())

            provider.signOut()

            assertNull(provider.getIdToken())
        }

    @Test
    fun `currentRefreshToken is null before sign-in and reflects the session afterward`() =
        runTest {
            val provider = provider(backgroundScope) { respondJson(SIGN_IN_JSON) }
            assertNull(provider.currentRefreshToken())

            assertIs<Result.Success<*>>(provider.signInWithGoogle("google-tok"))

            assertEquals("refresh-1", provider.currentRefreshToken())
        }

    @Test
    fun `restoreSession rebuilds a session from a persisted refresh token with no interactive call`() =
        runTest {
            var seenPath: String? = null
            var seenBody: String? = null
            val provider = provider(backgroundScope) { request ->
                seenPath = request.url.encodedPath
                seenBody = (request.body as? io.ktor.http.content.TextContent)?.text
                respondJson(REFRESH_JSON)
            }

            val result = provider.restoreSession("persisted-refresh-token")

            assertIs<Result.Success<Unit>>(result)
            assertTrue(seenPath?.endsWith("/v1/token") == true)
            assertTrue(seenBody?.contains("persisted-refresh-token") == true)
            assertEquals("labs-id-2", provider.getIdToken())
            assertEquals("refresh-2", provider.currentRefreshToken())
        }

    @Test
    fun `restoreSession maps an HTTP 400 to an authoritative Token Invalid error`() =
        runTest {
            val provider = provider(backgroundScope) {
                respond(
                    content = """{"error":{"message":"INVALID_REFRESH_TOKEN"}}""",
                    status = HttpStatusCode.BadRequest,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }

            val result = provider.restoreSession("revoked-refresh-token")

            assertIs<Result.Error<AuthError>>(result)
            assertIs<AuthError.Token.Invalid>(result.error)
            assertNull(provider.getIdToken())
        }

    @Test
    fun `restoreSession maps a network exception to a transient NetworkError - not Invalid`() =
        runTest {
            val provider = provider(backgroundScope) { throw RuntimeException("boom") }

            val result = provider.restoreSession("some-refresh-token")

            assertIs<Result.Error<AuthError>>(result)
            assertIs<AuthError.SignIn.NetworkError>(result.error)
        }

    @Test
    fun `restoreSession maps a 5xx server error to a transient NetworkError - not Invalid`() =
        runTest {
            val provider = provider(backgroundScope) {
                respond(
                    content = "",
                    status = HttpStatusCode.InternalServerError,
                )
            }

            val result = provider.restoreSession("some-refresh-token")

            assertIs<Result.Error<AuthError>>(result)
            assertIs<AuthError.SignIn.NetworkError>(result.error)
        }

    @Test
    fun `restoreSession with a blank api key reports NotConfigured and makes no call`() =
        runTest {
            var called = false
            val provider = provider(backgroundScope, apiKey = "") {
                called = true
                respondJson(REFRESH_JSON)
            }

            val result = provider.restoreSession("some-refresh-token")

            assertIs<Result.Error<AuthError>>(result)
            assertIs<AuthError.Configuration.NotConfigured>(result.error)
            assertTrue(!called)
        }

    // region getToken (typed outcomes, forced refresh, single-flight)

    @Test
    fun `getToken distinguishes no-session from a transient refresh failure`() =
        runTest {
            var nowMs = 1_000_000L
            var failRefresh = false
            val provider = provider(backgroundScope, now = { nowMs }) { request ->
                if (request.url.host.contains("securetoken")) {
                    if (failRefresh) throw RuntimeException("network down") else respondJson(REFRESH_JSON)
                } else {
                    respondJson(SIGN_IN_JSON)
                }
            }

            // No session at all -> NoSession.
            assertIs<FirebaseIdTokenProvider.TokenOutcome.NoSession>(provider.getToken())

            // Session present but the (needed) refresh fails on the network -> TransientFailure,
            // and the session is kept for a later retry.
            assertIs<Result.Success<*>>(provider.signInWithGoogle("google-tok"))
            nowMs += 3_600_000L
            failRefresh = true
            assertIs<FirebaseIdTokenProvider.TokenOutcome.TransientFailure>(provider.getToken())

            // The network recovers -> the same session refreshes fine.
            failRefresh = false
            val recovered = provider.getToken()
            assertIs<FirebaseIdTokenProvider.TokenOutcome.Token>(recovered)
            assertEquals("labs-id-2", recovered.idToken)
        }

    @Test
    fun `getToken forceRefresh bypasses an unexpired cached token`() =
        runTest {
            var refreshCalls = 0
            val provider = provider(backgroundScope) { request ->
                if (request.url.host.contains("securetoken")) {
                    refreshCalls++
                    respondJson(REFRESH_JSON)
                } else {
                    respondJson(SIGN_IN_JSON)
                }
            }
            assertIs<Result.Success<*>>(provider.signInWithGoogle("google-tok"))

            val cached = provider.getToken()
            assertIs<FirebaseIdTokenProvider.TokenOutcome.Token>(cached)
            assertTrue(cached.servedFromCache)
            assertEquals(0, refreshCalls)

            val forced = provider.getToken(forceRefresh = true)
            assertIs<FirebaseIdTokenProvider.TokenOutcome.Token>(forced)
            assertTrue(!forced.servedFromCache)
            assertEquals("labs-id-2", forced.idToken)
            assertEquals(1, refreshCalls)
        }

    @Test
    fun `getToken maps an authoritatively rejected refresh to Invalid and clears the session`() =
        runTest {
            var nowMs = 1_000_000L
            val provider = provider(backgroundScope, now = { nowMs }) { request ->
                if (request.url.host.contains("securetoken")) {
                    respond(
                        content = """{"error":{"message":"TOKEN_EXPIRED"}}""",
                        status = HttpStatusCode.BadRequest,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                } else {
                    respondJson(SIGN_IN_JSON)
                }
            }
            assertIs<Result.Success<*>>(provider.signInWithGoogle("google-tok"))

            nowMs += 3_600_000L
            assertIs<FirebaseIdTokenProvider.TokenOutcome.Invalid>(provider.getToken())
            assertIs<FirebaseIdTokenProvider.TokenOutcome.NoSession>(provider.getToken(), "the dead session must be cleared")
        }

    /**
     * Single-flight: concurrent callers that all need a refresh must share ONE securetoken call
     * (the refresh runs off-mutex, so without the shared in-flight Deferred each caller would
     * fire its own). Against the pre-fix code this still passed only because the mutex was held
     * across the network call — serializing every caller behind the network; this pins the
     * concurrency-friendly version.
     */
    @Test
    fun `concurrent getToken callers share a single refresh`() =
        runTest {
            var nowMs = 1_000_000L
            var refreshCalls = 0
            val provider = provider(backgroundScope, now = { nowMs }) { request ->
                if (request.url.host.contains("securetoken")) {
                    refreshCalls++
                    delay(50) // hold the refresh open so every caller piles onto it
                    respondJson(REFRESH_JSON)
                } else {
                    respondJson(SIGN_IN_JSON)
                }
            }
            assertIs<Result.Success<*>>(provider.signInWithGoogle("google-tok"))
            nowMs += 3_600_000L

            val results = (1..20).map { async { provider.getToken() } }.awaitAll()

            assertEquals(1, refreshCalls, "twenty concurrent callers must share one refresh")
            results.forEach { outcome ->
                assertIs<FirebaseIdTokenProvider.TokenOutcome.Token>(outcome)
                assertEquals("labs-id-2", outcome.idToken)
            }
        }

    // endregion

    // region Rotated-refresh-token callback

    @Test
    fun `a silent refresh reports the rotated refresh token`() =
        runTest {
            var nowMs = 1_000_000L
            val rotations = mutableListOf<String>()
            val provider = provider(backgroundScope, now = { nowMs }, onRefreshTokenRotated = { rotations += it }) { request ->
                if (request.url.host.contains("securetoken")) {
                    respondJson(REFRESH_JSON) // rotates refresh-1 -> refresh-2
                } else {
                    respondJson(SIGN_IN_JSON)
                }
            }
            assertIs<Result.Success<*>>(provider.signInWithGoogle("google-tok"))
            assertEquals(listOf("refresh-1"), rotations, "sign-in reports its token for persistence")

            nowMs += 3_600_000L
            assertIs<FirebaseIdTokenProvider.TokenOutcome.Token>(provider.getToken())

            assertEquals(listOf("refresh-1", "refresh-2"), rotations, "the silent refresh must report the rotated token")
            assertEquals("refresh-2", provider.currentRefreshToken())
        }

    // endregion

    // region Proactive refresh (virtual time)

    /**
     * With proactive refresh enabled, the provider refreshes in the background at
     * `expiresAt - EXPIRY_BUFFER_MS` — before expiry — so a user-facing request never pays the
     * refresh latency itself. Against the pre-fix code (no proactive task) this fails: no
     * securetoken call ever happens without a request-path getToken.
     */
    @Test
    fun `proactive refresh renews the token before expiry`() =
        runTest {
            var refreshCalls = 0
            val provider = provider(backgroundScope, now = { currentTime }) { request ->
                if (request.url.host.contains("securetoken")) {
                    refreshCalls++
                    respondJson(REFRESH_JSON)
                } else {
                    respondJson(SIGN_IN_JSON) // expiresIn 3600s from t=0
                }
            }
            provider.setProactiveRefresh(true)
            assertIs<Result.Success<*>>(provider.signInWithGoogle("google-tok"))
            assertEquals(0, refreshCalls)

            // Just before the buffer boundary: still quiet.
            advanceTimeBy(3_600_000L - FirebaseIdTokenProvider.EXPIRY_BUFFER_MS - 1)
            assertEquals(0, refreshCalls)

            // Crossing the boundary fires the background refresh — before expiry. The HTTP round
            // trip runs on the mock engine's own dispatcher, so await it deterministically by
            // joining the in-flight refresh via getToken (single-flight: this never starts a
            // second one).
            advanceTimeBy(2)
            runCurrent()
            assertIs<FirebaseIdTokenProvider.TokenOutcome.Token>(provider.getToken())
            assertEquals(1, refreshCalls, "the proactive task must refresh at expiresAt - buffer")
            assertTrue(currentTime < 3_600_000L, "the refresh must land before the token expires")

            // The refreshed session reschedules itself: another cycle later it refreshes again.
            advanceTimeBy(3_600_000L)
            runCurrent()
            assertIs<FirebaseIdTokenProvider.TokenOutcome.Token>(provider.getToken())
            assertEquals(2, refreshCalls, "proactive refresh must self-perpetuate")
        }

    @Test
    fun `sign-out cancels the scheduled proactive refresh`() =
        runTest {
            var refreshCalls = 0
            val provider = provider(backgroundScope, now = { currentTime }) { request ->
                if (request.url.host.contains("securetoken")) {
                    refreshCalls++
                    respondJson(REFRESH_JSON)
                } else {
                    respondJson(SIGN_IN_JSON)
                }
            }
            provider.setProactiveRefresh(true)
            assertIs<Result.Success<*>>(provider.signInWithGoogle("google-tok"))

            provider.signOut()
            advanceTimeBy(100_000_000L)
            advanceUntilIdle()

            assertEquals(0, refreshCalls, "sign-out must cancel the scheduled proactive refresh")
        }

    @Test
    fun `disabling proactive refresh cancels the scheduled task`() =
        runTest {
            var refreshCalls = 0
            val provider = provider(backgroundScope, now = { currentTime }) { request ->
                if (request.url.host.contains("securetoken")) {
                    refreshCalls++
                    respondJson(REFRESH_JSON)
                } else {
                    respondJson(SIGN_IN_JSON)
                }
            }
            provider.setProactiveRefresh(true)
            assertIs<Result.Success<*>>(provider.signInWithGoogle("google-tok"))

            provider.setProactiveRefresh(false) // e.g. the user switched to the other environment
            advanceTimeBy(100_000_000L)

            assertEquals(0, refreshCalls)
        }

    // endregion

    private companion object {
        fun provider(
            scope: CoroutineScope,
            apiKey: String = "test-api-key",
            now: () -> Long = { 1_000_000L },
            onRefreshTokenRotated: suspend (String) -> Unit = {},
            handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
        ): FirebaseIdTokenProvider {
            val client = HttpClient(MockEngine(handler)) {
                install(ContentNegotiation) { json(syncJson) }
            }
            return FirebaseIdTokenProvider(
                httpClient = client,
                apiKey = apiKey,
                scope = scope,
                now = now,
                onRefreshTokenRotated = onRefreshTokenRotated,
            )
        }

        fun MockRequestHandleScope.respondJson(content: String): HttpResponseData =
            respond(
                content = content,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )

        val SIGN_IN_JSON =
            """
            {"idToken":"labs-id-1","refreshToken":"refresh-1","expiresIn":"3600","localId":"uid1","email":"a@example.com"}
            """.trimIndent()

        val REFRESH_JSON =
            """
            {"id_token":"labs-id-2","refresh_token":"refresh-2","expires_in":"3600"}
            """.trimIndent()
    }
}
