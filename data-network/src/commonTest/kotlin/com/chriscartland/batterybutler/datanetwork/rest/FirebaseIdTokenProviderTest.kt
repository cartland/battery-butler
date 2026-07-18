package com.chriscartland.batterybutler.datanetwork.rest

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
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FirebaseIdTokenProviderTest {
    @Test
    fun `signInWithGoogle exchanges the Google token and getIdToken returns the Labs id token`() =
        runTest {
            var seenKey: String? = null
            var seenPath: String? = null
            var seenBody: String? = null
            val provider = provider { request ->
                seenKey = request.url.parameters["key"]
                seenPath = request.url.encodedPath
                seenBody = (request.body as? io.ktor.http.content.TextContent)?.text
                respondJson(SIGN_IN_JSON)
            }

            val result = provider.signInWithGoogle("google-tok")

            assertIs<Result.Success<Unit>>(result)
            assertEquals("test-api-key", seenKey)
            assertTrue(seenPath?.endsWith("accounts:signInWithIdp") == true)
            assertTrue(seenBody?.contains("id_token=google-tok&providerId=google.com") == true)
            assertEquals("labs-id-1", provider.getIdToken())
        }

    @Test
    fun `getIdToken is null before sign-in`() =
        runTest {
            val provider = provider { respondJson(SIGN_IN_JSON) }
            assertNull(provider.getIdToken())
        }

    @Test
    fun `getIdToken refreshes via securetoken when the id token is near expiry`() =
        runTest {
            var nowMs = 1_000_000L
            var refreshCalls = 0
            val provider = provider(now = { nowMs }) { request ->
                if (request.url.host.contains("securetoken")) {
                    refreshCalls++
                    respondJson(REFRESH_JSON)
                } else {
                    respondJson(SIGN_IN_JSON) // expiresIn 3600 -> expiresAt = now + 3_600_000
                }
            }

            assertIs<Result.Success<Unit>>(provider.signInWithGoogle("google-tok"))
            assertEquals("labs-id-1", provider.getIdToken()) // fresh, no refresh
            assertEquals(0, refreshCalls)

            nowMs += 3_600_000L // past expiry (minus the 60s buffer)
            assertEquals("labs-id-2", provider.getIdToken()) // refreshed
            assertEquals(1, refreshCalls)
        }

    @Test
    fun `signInWithGoogle with a blank api key reports NotConfigured and makes no call`() =
        runTest {
            var called = false
            val provider = provider(apiKey = "") {
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
            val provider = provider {
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
            val provider = provider { throw RuntimeException("boom") }

            val result = provider.signInWithGoogle("google-tok")

            assertIs<Result.Error<AuthError>>(result)
            assertIs<AuthError.SignIn.NetworkError>(result.error)
        }

    @Test
    fun `signOut clears the session`() =
        runTest {
            val provider = provider { respondJson(SIGN_IN_JSON) }
            assertIs<Result.Success<Unit>>(provider.signInWithGoogle("google-tok"))
            assertEquals("labs-id-1", provider.getIdToken())

            provider.signOut()

            assertNull(provider.getIdToken())
        }

    @Test
    fun `currentRefreshToken is null before sign-in and reflects the session afterward`() =
        runTest {
            val provider = provider { respondJson(SIGN_IN_JSON) }
            assertNull(provider.currentRefreshToken())

            assertIs<Result.Success<Unit>>(provider.signInWithGoogle("google-tok"))

            assertEquals("refresh-1", provider.currentRefreshToken())
        }

    @Test
    fun `restoreSession rebuilds a session from a persisted refresh token with no interactive call`() =
        runTest {
            var seenPath: String? = null
            var seenBody: String? = null
            val provider = provider { request ->
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
            val provider = provider {
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
    fun `restoreSession maps a network exception to a transient NetworkError, not Invalid`() =
        runTest {
            val provider = provider { throw RuntimeException("boom") }

            val result = provider.restoreSession("some-refresh-token")

            assertIs<Result.Error<AuthError>>(result)
            assertIs<AuthError.SignIn.NetworkError>(result.error)
        }

    @Test
    fun `restoreSession maps a 5xx server error to a transient NetworkError, not Invalid`() =
        runTest {
            val provider = provider {
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
            val provider = provider(apiKey = "") {
                called = true
                respondJson(REFRESH_JSON)
            }

            val result = provider.restoreSession("some-refresh-token")

            assertIs<Result.Error<AuthError>>(result)
            assertIs<AuthError.Configuration.NotConfigured>(result.error)
            assertTrue(!called)
        }

    private companion object {
        fun provider(
            apiKey: String = "test-api-key",
            now: () -> Long = { 1_000_000L },
            handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
        ): FirebaseIdTokenProvider {
            val client = HttpClient(MockEngine(handler)) {
                install(ContentNegotiation) { json(syncJson) }
            }
            return FirebaseIdTokenProvider(httpClient = client, apiKey = apiKey, now = now)
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
