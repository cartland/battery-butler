package com.chriscartland.batterybutler.presentation.core.util

import androidx.compose.runtime.compositionLocalOf

interface ShareHandler {
    fun shareText(text: String)
}

val LocalShareHandler = compositionLocalOf<ShareHandler> {
    error("No ShareHandler provided")
}
