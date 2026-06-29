package com.chriscartland.batterybutler.domain.model

/**
 * Wraps the Labs Firebase **Web API key** so it can be injected without ambiguity.
 *
 * Used by the Labs REST sync auth ([FirebaseIdTokenProvider] in :data-network) to exchange a
 * Google ID token for a Labs Firebase ID token. Blank when unconfigured (owner setup pending).
 */
data class LabsFirebaseApiKey(
    val apiKey: String,
)
