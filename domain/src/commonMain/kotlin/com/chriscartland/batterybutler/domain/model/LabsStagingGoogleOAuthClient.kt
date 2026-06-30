package com.chriscartland.batterybutler.domain.model

/**
 * The Google OAuth client used to sign in to **Labs staging** (Workstream E config).
 *
 * The Labs sign-in mints a Google ID token for this client — whose audience the Labs *staging*
 * Firebase project trusts — then exchanges it for a Labs Firebase ID token. [clientSecret] is sent
 * at token exchange for Google "Desktop app" clients (it is not confidential for installed apps);
 * blank when unconfigured (owner setup pending) or when the platform doesn't use one.
 */
data class LabsStagingGoogleOAuthClient(
    val clientId: String,
    val clientSecret: String,
)
