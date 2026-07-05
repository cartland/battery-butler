package com.chriscartland.batterybutler.presentationcore.util

import androidx.compose.runtime.compositionLocalOf

/**
 * Copies short-lived credentials (tokens, not general text) to the system clipboard, using
 * whatever platform-specific "sensitive content" treatment is available so the value doesn't
 * linger in clipboard history or preview overlays the way a copied password wouldn't.
 */
interface SecureClipboard {
    fun copySensitive(text: String)
}

val LocalSecureClipboard = compositionLocalOf<SecureClipboard> {
    error("No SecureClipboard provided")
}
