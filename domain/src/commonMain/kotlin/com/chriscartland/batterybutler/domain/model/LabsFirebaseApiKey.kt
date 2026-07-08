package com.chriscartland.batterybutler.domain.model

/**
 * Wraps the Labs Firebase **Web API keys** so they can be injected without ambiguity.
 *
 * Per-env because staging (`cartland-labs-staging`) and prod (`cartland-labs`) are separate Firebase
 * projects with separate (public) Web API keys. The Labs REST sync auth ([FirebaseIdTokenProvider]
 * in :data-network) uses the key matching the current [DataMode] to exchange a Google ID token
 * for a Labs Firebase ID token minted by that env's project. Either may be blank when unconfigured
 * (owner setup pending).
 */
data class LabsFirebaseApiKey(
    val staging: String,
    val prod: String,
)
