package com.chriscartland.batterybutler.domain.model

/**
 * Represents an authenticated user.
 *
 * @property id Unique identifier for the user (from the auth provider).
 * @property email User's email address, if available.
 * @property displayName User's display name, if available.
 * @property photoUrl URL to the user's profile photo, if available.
 */
data class User(
    val id: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
) {
    init {
        require(id.isNotBlank()) { "User ID cannot be blank" }
    }
}
