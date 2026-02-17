package com.chriscartland.batterybutler.server.app

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.proto.AuthServiceGrpcKt
import com.chriscartland.batterybutler.proto.VerifyTokenRequest
import com.chriscartland.batterybutler.proto.VerifyTokenResponse
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AuthService : AuthServiceGrpcKt.AuthServiceCoroutineImplBase() {
    private val log = Logger.withTag("AuthService")

    private val clientId: String? = System.getenv("GOOGLE_CLIENT_ID")

    private val verifier: GoogleIdTokenVerifier? = clientId?.let {
        GoogleIdTokenVerifier
            .Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
            ).setAudience(listOf(it))
            .build()
    }

    // In-memory session store. Sessions are lost on restart.
    // Replace with DB-backed store when persistence is needed.
    private val sessions = ConcurrentHashMap<String, SessionInfo>()

    private val e2eTestToken: String? = System.getenv("E2E_TEST_TOKEN")

    init {
        if (clientId == null) {
            log.w { "GOOGLE_CLIENT_ID not set — VerifyToken will reject all requests" }
        } else {
            log.i { "AuthService initialized with Google Client ID" }
        }
        if (e2eTestToken != null) {
            if (e2eTestToken.isBlank()) {
                log.w { "E2E_TEST_TOKEN is set but blank — ignoring" }
            } else {
                sessions[e2eTestToken] = SessionInfo(
                    userId = E2E_TEST_USER_ID,
                    email = E2E_TEST_EMAIL,
                    expiresAtMs = Long.MAX_VALUE,
                )
                log.i { "E2E test token mode ACTIVE — synthetic test session registered" }
            }
        }
    }

    override suspend fun verifyToken(request: VerifyTokenRequest): VerifyTokenResponse {
        if (verifier == null) {
            return errorResponse("Server not configured for Google token verification")
        }

        val googleIdToken = request.googleIdToken
        if (googleIdToken.isBlank()) {
            return errorResponse("Google ID token is required")
        }

        return try {
            val idToken = withContext(Dispatchers.IO) {
                verifier.verify(googleIdToken)
            }
            if (idToken == null) {
                log.w { "Token verification failed — invalid token" }
                errorResponse("Invalid Google ID token")
            } else {
                val payload = idToken.payload
                val userId = payload.subject
                val email = payload.email ?: ""
                val name = payload["name"] as? String ?: ""
                val pictureUrl = payload["picture"] as? String ?: ""

                val sessionToken = UUID.randomUUID().toString()
                val expiresAtMs = System.currentTimeMillis() + SESSION_EXPIRY_MS

                sessions[sessionToken] = SessionInfo(
                    userId = userId,
                    email = email,
                    expiresAtMs = expiresAtMs,
                )

                log.i { "Token verified for $email (userId=$userId)" }

                VerifyTokenResponse
                    .newBuilder()
                    .setValid(true)
                    .setSessionToken(sessionToken)
                    .setUserId(userId)
                    .setEmail(email)
                    .setDisplayName(name)
                    .setPhotoUrl(pictureUrl)
                    .setExpiresAtMs(expiresAtMs)
                    .build()
            }
        } catch (e: Exception) {
            log.e(e) { "Token verification error" }
            errorResponse("Token verification failed: ${e.message}")
        }
    }

    /**
     * Validate a session token. Returns the session info if valid, null otherwise.
     * Used by the auth interceptor.
     */
    fun validateSession(sessionToken: String): SessionInfo? {
        val session = sessions[sessionToken] ?: return null
        if (System.currentTimeMillis() > session.expiresAtMs) {
            sessions.remove(sessionToken)
            return null
        }
        return session
    }

    private fun errorResponse(message: String): VerifyTokenResponse =
        VerifyTokenResponse
            .newBuilder()
            .setValid(false)
            .setErrorMessage(message)
            .build()

    data class SessionInfo(
        val userId: String,
        val email: String,
        val expiresAtMs: Long,
    )

    companion object {
        private const val SESSION_EXPIRY_MS = 24 * 60 * 60 * 1000L // 24 hours
        const val E2E_TEST_USER_ID = "e2e-test-user"
        const val E2E_TEST_EMAIL = "e2e-test@test.local"
    }
}
