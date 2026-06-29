package com.chriscartland.batterybutler.datanetwork.rest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Wire DTOs for the Firebase Auth REST flow used to authenticate against the Labs backend
 * (see [FirebaseIdTokenProvider]). Two Google endpoints, two casing conventions:
 *   - accounts:signInWithIdp  -> camelCase (idToken, refreshToken, expiresIn)
 *   - securetoken /v1/token   -> snake_case (id_token, refresh_token, expires_in)
 *
 * All response fields default so an additive field from a newer API can't break parsing
 * (the lenient-consumer side of the defensive wire boundary; syncJson has ignoreUnknownKeys).
 */

/** Request body for `accounts:signInWithIdp` — exchanges a Google ID token for a Labs session. */
@Serializable
internal data class SignInWithIdpRequest(
    // "id_token=<googleIdToken>&providerId=google.com"
    val postBody: String,
    val requestUri: String = "http://localhost",
    val returnSecureToken: Boolean = true,
)

/** Response from `accounts:signInWithIdp` (camelCase). */
@Serializable
internal data class SignInWithIdpResponse(
    val idToken: String = "",
    val refreshToken: String = "",
    val expiresIn: String = "0",
    val localId: String = "",
    val email: String = "",
)

/** Request body for the securetoken `/v1/token` refresh endpoint. */
@Serializable
internal data class RefreshTokenRequest(
    @SerialName("grant_type") val grantType: String = "refresh_token",
    @SerialName("refresh_token") val refreshToken: String,
)

/** Response from securetoken `/v1/token` (snake_case). */
@Serializable
internal data class RefreshTokenResponse(
    @SerialName("id_token") val idToken: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("expires_in") val expiresIn: String = "0",
)
