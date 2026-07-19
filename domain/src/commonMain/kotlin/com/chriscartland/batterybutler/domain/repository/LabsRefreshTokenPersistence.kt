package com.chriscartland.batterybutler.domain.repository

/**
 * Persists the Labs Firebase refresh token per environment (staging/prod, keyed the same way as
 * [com.chriscartland.batterybutler.domain.model.DataModeKeyedState] / `apiKeyForMode`), so a real
 * Labs session can be restored non-interactively after a process restart via a plain network call
 * instead of falling back to Google's Credential Manager -- which is not actually guaranteed
 * headless on Android (see `bb-silent-reauth-cooldown` in TODO.md, the bug this replaces).
 *
 * A thin key-value port defined here (not in `data-local`) so `data-network`'s
 * `DefaultLabsAuthGateway` can depend on it without taking a dependency on the storage layer --
 * both modules already depend on `domain`. Deliberately not folded into the richer
 * `LabsSessionStorage` UI-belief store, which is documented to hold no tokens. See
 * `bb-labs-refresh-token-persistence` in TODO.md.
 */
interface LabsRefreshTokenPersistence {
    /** The persisted refresh token for [environmentKey], or null if never signed in / cleared. */
    suspend fun get(environmentKey: String): String?

    /** Persists [refreshToken] for [environmentKey], overwriting any previous value. */
    suspend fun save(
        environmentKey: String,
        refreshToken: String,
    )

    /** Clears the persisted refresh token for [environmentKey]. */
    suspend fun clear(environmentKey: String)
}
