package com.chriscartland.batterybutler.presentationcore.util

import androidx.compose.runtime.compositionLocalOf

/**
 * Platform-specific app restart hook. Used after Settings → Restore Previous Data
 * succeeds (or destructively migrates) to force a clean reload of all data-bound
 * Flows by restarting the process — see bd issue bb-lg42 for why an in-process
 * StateFlow swap isn't sufficient on Android in practice.
 *
 * Implementations:
 *   - Android: kills the process and re-launches the launcher Intent
 *   - iOS / Desktop: no-op (user dismisses the snackbar and reopens manually)
 */
fun interface AppRestarter {
    /**
     * Trigger an app restart. On platforms that don't support clean process
     * restart (iOS), implementations should no-op; the snackbar is responsible
     * for telling the user to reopen the app manually.
     */
    fun restart()
}

val LocalAppRestarter = compositionLocalOf<AppRestarter> {
    AppRestarter { /* no-op default for previews and unsupported platforms */ }
}
