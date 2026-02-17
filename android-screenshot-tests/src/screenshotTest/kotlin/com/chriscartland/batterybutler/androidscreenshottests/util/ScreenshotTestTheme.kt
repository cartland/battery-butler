package com.chriscartland.batterybutler.androidscreenshottests.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.chriscartland.batterybutler.composeresources.LocalAppStrings

import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme

/**
 * Theme wrapper for screenshot tests.
 *
 * This composable injects the [ScreenshotAppStrings] implementation into the [LocalAppStrings]
 * CompositionLocal. It also applies the [BatteryButlerTheme] to ensure that screenshot tests
 * match the production app's visual style.
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
    BatteryButlerTheme {
        CompositionLocalProvider(LocalAppStrings provides ScreenshotAppStrings()) {
            content()
        }
    }
}
