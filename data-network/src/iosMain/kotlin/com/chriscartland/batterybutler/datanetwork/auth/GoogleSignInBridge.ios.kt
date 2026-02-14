package com.chriscartland.batterybutler.datanetwork.auth

import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.Result
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import kotlin.coroutines.resume
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

/**
 * iOS implementation of [GoogleSignInBridge].
 *
 * Uses ASWebAuthenticationSession for OAuth 2.0 PKCE flow.
 * Opens a system browser sheet for Google sign-in, then exchanges
 * the authorization code for an ID token via Google's token endpoint.
 *
 * Requires an iOS client ID from Google Cloud Console configured
 * with a custom URL scheme redirect (reversed client ID).
 */
actual class GoogleSignInBridge {
    private var clientId: String? = null
    private var redirectUri: String? = null

    /**
     * Initializes the bridge with the client ID.
     *
     * @param clientId The OAuth 2.0 iOS Client ID from Google Cloud Console.
     *   The redirect URI is derived from the reversed client ID.
     */
    fun initialize(clientId: String?) {
        this.clientId = clientId
        if (clientId.isNullOrBlank()) {
            println("Google Sign-In (iOS): Not configured - set GOOGLE_IOS_CLIENT_ID")
        } else {
            // Google's iOS redirect URI uses the reversed client ID as a custom URL scheme
            val reversedClientId = clientId.split(".").reversed().joinToString(".")
            this.redirectUri = "$reversedClientId:/oauthredirect"
            println("Google Sign-In (iOS): Configured with client ID ...${clientId.takeLast(15)}")
        }
    }

    actual suspend fun signIn(): Result<GoogleIdToken, AuthError.SignIn> {
        val id = clientId
        if (id.isNullOrBlank()) {
            return Result.Error(
                AuthError.SignIn.Failed(
                    message = "Google Sign-In not configured",
                    cause = "iOS client ID is not configured",
                ),
            )
        }

        val redirect = redirectUri ?: return Result.Error(
            AuthError.SignIn.Failed(
                message = "Sign-in not configured",
                cause = "Redirect URI not set",
            ),
        )

        // Generate PKCE code verifier and challenge
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)

        // Build authorization URL
        val authUrl = buildAuthUrl(
            clientId = id,
            redirectUri = redirect,
            codeChallenge = codeChallenge,
        )

        // Get the custom URL scheme for callback detection
        val callbackScheme = redirect.substringBefore(":")

        // Launch ASWebAuthenticationSession
        val callbackUrl = launchAuthSession(authUrl, callbackScheme)
            ?: return Result.Error(
                AuthError.SignIn.Cancelled(
                    message = "Sign-in cancelled",
                    cause = "User dismissed the sign-in sheet",
                ),
            )

        // Extract authorization code from callback URL
        val code = extractAuthCode(callbackUrl)
            ?: return Result.Error(
                AuthError.SignIn.Failed(
                    message = "Sign-in failed",
                    cause = "No authorization code in callback",
                ),
            )

        // Exchange code for tokens
        return exchangeCodeForToken(
            code = code,
            clientId = id,
            redirectUri = redirect,
            codeVerifier = codeVerifier,
        )
    }

    actual suspend fun signOut() {
        // OAuth PKCE flow is stateless on the client side.
        // The auth repository handles clearing stored tokens.
    }

    actual fun isConfigured(): Boolean = !clientId.isNullOrBlank()

    @OptIn(ExperimentalEncodingApi::class)
    private fun generateCodeVerifier(): String {
        val bytes = Random.nextBytes(32)
        return Base64.UrlSafe.encode(bytes).trimEnd('=')
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun generateCodeChallenge(verifier: String): String {
        // Use SHA-256 hash of verifier for S256 challenge
        val bytes = verifier.encodeToByteArray()
        val digest = sha256(bytes)
        return Base64.UrlSafe.encode(digest).trimEnd('=')
    }

    private fun buildAuthUrl(
        clientId: String,
        redirectUri: String,
        codeChallenge: String,
    ): String {
        val params = listOf(
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "response_type" to "code",
            "scope" to "openid email profile",
            "code_challenge" to codeChallenge,
            "code_challenge_method" to "S256",
        ).joinToString("&") { (k, v) ->
            "$k=${urlEncode(v)}"
        }
        return "https://accounts.google.com/o/oauth2/v2/auth?$params"
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun launchAuthSession(
        authUrl: String,
        callbackScheme: String,
    ): String? =
        suspendCancellableCoroutine { cont ->
            val url = NSURL(string = authUrl)
            val session = ASWebAuthenticationSession(
                uRL = url,
                callbackURLScheme = callbackScheme,
            ) { callbackURL: NSURL?, _: NSError? ->
                if (cont.isActive) {
                    cont.resume(callbackURL?.absoluteString)
                }
            }
            session.prefersEphemeralWebBrowserSession = true
            session.presentationContextProvider = DefaultPresentationContextProvider()
            session.start()

            cont.invokeOnCancellation {
                session.cancel()
            }
        }

    private fun extractAuthCode(callbackUrl: String): String? {
        // URL format: scheme:/oauthredirect?code=...&scope=...
        val queryStart = callbackUrl.indexOf('?')
        if (queryStart < 0) return null
        val query = callbackUrl.substring(queryStart + 1)
        return query
            .split("&")
            .map { it.split("=", limit = 2) }
            .firstOrNull { it.size == 2 && it[0] == "code" }
            ?.get(1)
    }

    private suspend fun exchangeCodeForToken(
        code: String,
        clientId: String,
        redirectUri: String,
        codeVerifier: String,
    ): Result<GoogleIdToken, AuthError.SignIn> {
        val client = HttpClient(Darwin)
        return try {
            val response = client.submitForm(
                url = "https://oauth2.googleapis.com/token",
                formParameters = Parameters.build {
                    append("code", code)
                    append("client_id", clientId)
                    append("redirect_uri", redirectUri)
                    append("grant_type", "authorization_code")
                    append("code_verifier", codeVerifier)
                },
            )

            if (!response.status.isSuccess()) {
                return Result.Error(
                    AuthError.SignIn.Failed(
                        message = "Token exchange failed",
                        cause = "HTTP ${response.status.value}",
                    ),
                )
            }

            val body = response.bodyAsText()
            val idToken = extractJsonString(body, "id_token")
                ?: return Result.Error(
                    AuthError.SignIn.Failed(
                        message = "No ID token in response",
                        cause = "Token endpoint did not return id_token",
                    ),
                )

            // Decode JWT payload to extract user info
            val claims = decodeJwtPayload(idToken)
            Result.Success(
                GoogleIdToken(
                    idToken = idToken,
                    email = extractJsonString(claims, "email"),
                    displayName = extractJsonString(claims, "name"),
                    photoUrl = extractJsonString(claims, "picture"),
                ),
            )
        } catch (e: Exception) {
            Result.Error(
                AuthError.SignIn.NetworkError(
                    message = "Network error during sign-in",
                    cause = e.message,
                ),
            )
        } finally {
            client.close()
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeJwtPayload(jwt: String): String {
        val parts = jwt.split(".")
        if (parts.size < 2) return "{}"
        val payload = parts[1]
        // Add padding if needed
        val padded = when (payload.length % 4) {
            2 -> "$payload=="
            3 -> "$payload="
            else -> payload
        }
        return Base64.UrlSafe.decode(padded).decodeToString()
    }

    private fun extractJsonString(
        json: String,
        key: String,
    ): String? {
        // Simple JSON string extraction without a JSON library dependency
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\""
        val match = Regex(pattern).find(json)
        return match?.groupValues?.getOrNull(1)
    }

    private fun urlEncode(value: String): String =
        value
            .replace(" ", "%20")
            .replace(":", "%3A")
            .replace("/", "%2F")
            .replace("@", "%40")

    @OptIn(ExperimentalForeignApi::class)
    private fun sha256(input: ByteArray): ByteArray {
        val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
        input.usePinned { pinned ->
            digest.usePinned { digestPinned ->
                CC_SHA256(pinned.addressOf(0), input.size.convert(), digestPinned.addressOf(0))
            }
        }
        return digest.toByteArray()
    }
}

/**
 * Provides the presentation context (window) for ASWebAuthenticationSession.
 */
@Suppress("CONFLICTING_OVERLOADS")
private class DefaultPresentationContextProvider :
    platform.darwin.NSObject(),
    ASWebAuthenticationPresentationContextProvidingProtocol {
    override fun presentationAnchorForWebAuthenticationSession(
        session: ASWebAuthenticationSession,
    ): UIWindow {
        // Find the key window from connected scenes (modern iOS 15+ API)
        val scenes = UIApplication.sharedApplication.connectedScenes
        for (scene in scenes) {
            val windowScene = scene as? UIWindowScene ?: continue
            val window = windowScene.windows.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true }
            if (window != null) return window as UIWindow
        }
        return UIWindow()
    }
}
