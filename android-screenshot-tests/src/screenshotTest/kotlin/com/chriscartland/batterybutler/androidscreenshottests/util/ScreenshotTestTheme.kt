package com.chriscartland.batterybutler.androidscreenshottests.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.chriscartland.batterybutler.presentationcore.resources.LocalAppStrings

@Composable
fun ScreenshotTestTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppStrings provides ScreenshotAppStrings()) {
        content()
    }
}
