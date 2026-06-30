package com.chriscartland.batterybutler.domain.model

/**
 * The Google OAuth client used to sign in to **Labs prod** (Workstream E config).
 *
 * Prod counterpart of [LabsStagingGoogleOAuthClient] — a separate client because staging and prod
 * are separate Firebase projects. Blank when unconfigured.
 */
data class LabsProdGoogleOAuthClient(
    val clientId: String,
    val clientSecret: String,
)
