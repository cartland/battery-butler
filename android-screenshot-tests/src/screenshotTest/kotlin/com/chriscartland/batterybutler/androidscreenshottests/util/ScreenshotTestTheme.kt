package com.chriscartland.batterybutler.androidscreenshottests.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.chriscartland.batterybutler.presentationcore.resources.LocalAppStrings

/**
 * Theme wrapper for screenshot tests.
 *
 * This composable injects the [ScreenshotAppStrings] implementation into the [LocalAppStrings]
 * CompositionLocal. This ensures that any composables running within this theme (i.e., previews
 * being screenshot-tested) can resolve string resources correctly using the local XML file
 * instead of failing or showing placeholders.
 *
 * Usage:
 * ```
 * ScreenshotTestTheme {
 *     MyComposablePreview()
 * }
 * ```
 * Note: Always place the content lambda on a new line to satisfy formatting rules.
 */
@Composable
fun ScreenshotTestTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppStrings provides ScreenshotAppStrings()) {
        content()
    }
}
