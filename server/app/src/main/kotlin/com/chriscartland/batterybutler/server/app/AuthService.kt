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

    init {
        if (clientId == null) {
            log.w { "GOOGLE_CLIENT_ID not set — VerifyToken will reject all requests" }
        } else {
            log.i { "AuthService initialized with Google Client ID" }
        }
    }

    override suspend fun verifyToken(request: VerifyTokenRequest): VerifyTokenResponse {
        if (verifier == null) {
            log.w { "Dev Mode: Skipping Google verification for token '${request.googleIdToken}'" }
            // In Dev Mode (no client ID), accept any token and return a dummy session
            return createSession("dev-user", "dev@example.com", "Dev User", "")
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

                createSession(userId, email, name, pictureUrl)
            }
        } catch (e: Exception) {
            log.e(e) { "Token verification error" }
            errorResponse("Token verification failed: ${e.message}")
        }
    }

    private fun createSession(
        userId: String,
        email: String,
        name: String,
        pictureUrl: String,
    ): VerifyTokenResponse {
        val sessionToken = UUID.randomUUID().toString()
        val expiresAtMs = System.currentTimeMillis() + SESSION_EXPIRY_MS

        sessions[sessionToken] = SessionInfo(
            userId = userId,
            email = email,
            expiresAtMs = expiresAtMs,
        )

        log.i { "Session created for $email (userId=$userId)" }

        return VerifyTokenResponse
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

    private companion object {
        const val SESSION_EXPIRY_MS = 24 * 60 * 60 * 1000L // 24 hours
    }
}
