package com.chriscartland.batterybutler.datanetwork.auth

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.DispatcherProvider
import com.chriscartland.batterybutler.domain.model.Result
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.awt.Desktop
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import kotlin.coroutines.resume

/**
 * Desktop (JVM) implementation of [GoogleSignInBridge].
 *
 * Uses OAuth 2.0 PKCE flow:
 * 1. Starts a local HTTP server on a random port
 * 2. Opens the browser to Google's OAuth consent page
 * 3. Receives the authorization code via redirect
 * 4. Exchanges the code for an ID token
 */
actual class GoogleSignInBridge {
    private var clientId: String? = null
    private var dispatcherProvider: DispatcherProvider? = null
    private val json = Json { ignoreUnknownKeys = true }

    actual fun initialize(
        clientId: String?,
        dispatcherProvider: DispatcherProvider?,
    ) {
        this.clientId = clientId
        this.dispatcherProvider = dispatcherProvider
        if (clientId.isNullOrBlank()) {
            Logger.w { "Google Sign-In (Desktop): Not configured" }
            Logger.w { "  Set GOOGLE_WEB_CLIENT_ID environment variable" }
        } else {
            Logger.i { "Google Sign-In (Desktop): Configured with client ID ...${clientId.takeLast(15)}" }
        }
    }

    actual suspend fun signIn(): Result<GoogleIdToken, AuthError.SignIn> {
        val id = clientId
        if (id.isNullOrBlank()) {
            return Result.Error(
                AuthError.SignIn.Failed(
                    message = "Google Sign-In not configured",
                    cause = "Client ID is not configured",
                ),
            )
        }

        return try {
            val codeVerifier = generateCodeVerifier()
            val codeChallenge = generateCodeChallenge(codeVerifier)

            // Start local server and get auth code
            val (authCode, redirectUri) = withContext(dispatcherProvider?.io ?: Dispatchers.IO) {
                awaitAuthCode(id, codeChallenge)
            }

            // Exchange code for tokens
            val tokenResponse = withContext(dispatcherProvider?.io ?: Dispatchers.IO) {
                exchangeCodeForTokens(authCode, id, codeVerifier, redirectUri)
            }

            val idToken = tokenResponse["id_token"]?.jsonPrimitive?.content
                ?: return Result.Error(
                    AuthError.SignIn.Failed(
                        message = "No ID token in response",
                        cause = "Token exchange succeeded but response contained no id_token",
                    ),
                )

            val claims = parseJwtPayload(idToken)
            Result.Success(
                GoogleIdToken(
                    idToken = idToken,
                    email = claims["email"]?.jsonPrimitive?.content,
                    displayName = claims["name"]?.jsonPrimitive?.content,
                    photoUrl = claims["picture"]?.jsonPrimitive?.content,
                ),
            )
        } catch (e: Exception) {
            Result.Error(
                AuthError.SignIn.Failed(
                    message = "Sign-in failed",
                    cause = e.message ?: "Unknown error",
                ),
            )
        }
    }

    actual suspend fun signOut() {
        // Desktop OAuth has no persistent session to clear beyond stored tokens
        // (handled by AuthTokenStorage in the repository layer)
    }

    actual fun isConfigured(): Boolean = !clientId.isNullOrBlank()

    /**
     * Starts a local HTTP server, opens the browser for Google consent,
     * and waits for the authorization code redirect.
     *
     * @return Pair of (authorization code, redirect URI used)
     */
    private suspend fun awaitAuthCode(
        clientId: String,
        codeChallenge: String,
    ): Pair<String, String> =
        suspendCancellableCoroutine { continuation ->
            val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            val port = server.address.port
            val redirectUri = "http://127.0.0.1:$port"

            server.createContext("/") { exchange ->
                val query = exchange.requestURI.query ?: ""
                val params = parseQueryParams(query)
                val code = params["code"]
                val error = params["error"]

                val html = when {
                    code != null -> SUCCESS_HTML
                    error != null -> CANCELLED_HTML
                    else -> ERROR_HTML
                }

                exchange.responseHeaders.set("Content-Type", "text/html; charset=utf-8")
                val bytes = html.toByteArray()
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }

                server.stop(1)

                when {
                    code != null -> continuation.resume(code to redirectUri)
                    error != null -> continuation.resume(
                        throw Exception("Google Sign-In cancelled: $error"),
                    )
                    else -> continuation.resume(
                        throw Exception("No authorization code received"),
                    )
                }
            }

            server.start()
            continuation.invokeOnCancellation { server.stop(0) }

            val authUrl = buildString {
                append("https://accounts.google.com/o/oauth2/v2/auth?")
                append("client_id=${encode(clientId)}")
                append("&redirect_uri=${encode(redirectUri)}")
                append("&response_type=code")
                append("&scope=${encode("openid email profile")}")
                append("&code_challenge=${encode(codeChallenge)}")
                append("&code_challenge_method=S256")
            }

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(authUrl))
            } else {
                println("Open this URL in your browser to sign in:")
                println(authUrl)
            }
        }

    private fun exchangeCodeForTokens(
        code: String,
        clientId: String,
        codeVerifier: String,
        redirectUri: String,
    ): JsonObject {
        val postData = buildString {
            append("code=${encode(code)}")
            append("&client_id=${encode(clientId)}")
            append("&redirect_uri=${encode(redirectUri)}")
            append("&grant_type=authorization_code")
            append("&code_verifier=${encode(codeVerifier)}")
        }

        val connection = URI("https://oauth2.googleapis.com/token").toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        connection.doOutput = true

        OutputStreamWriter(connection.outputStream).use { it.write(postData) }

        val responseCode = connection.responseCode
        val body = if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().readText()
        } else {
            val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: ""
            throw Exception("Token exchange failed ($responseCode): $errorBody")
        }

        return json.decodeFromString<JsonObject>(body)
    }

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    private fun parseJwtPayload(jwt: String): JsonObject {
        val parts = jwt.split(".")
        if (parts.size != 3) return JsonObject(emptyMap())
        val payload = Base64.getUrlDecoder().decode(parts[1])
        return json.decodeFromString<JsonObject>(String(payload))
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query
            .split("&")
            .mapNotNull { param ->
                val parts = param.split("=", limit = 2)
                if (parts.size == 2) parts[0] to URLDecoder.decode(parts[1], "UTF-8") else null
            }.toMap()
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    companion object {
        private val SUCCESS_HTML =
            """
            <html><body style="font-family: sans-serif; text-align: center; padding: 40px;">
            <h1>Sign-in successful</h1>
            <p>You can close this window and return to Battery Butler.</p>
            </body></html>
            """.trimIndent()

        private val CANCELLED_HTML =
            """
            <html><body style="font-family: sans-serif; text-align: center; padding: 40px;">
            <h1>Sign-in cancelled</h1>
            <p>You can close this window.</p>
            </body></html>
            """.trimIndent()

        private val ERROR_HTML =
            """
            <html><body style="font-family: sans-serif; text-align: center; padding: 40px;">
            <h1>Error</h1>
            <p>Unexpected response. You can close this window.</p>
            </body></html>
            """.trimIndent()
    }
}
