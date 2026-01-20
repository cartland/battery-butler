package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.androidscreenshottests.util.ScreenshotTestTheme
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBarPreview
import com.chriscartland.batterybutler.presentationcore.theme.LocalAiAvailable

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TopAppBarDefaultPreviewTest() {
    ScreenshotTestTheme {
        ButlerCenteredTopAppBarPreview()
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TopAppBarWithBackPreview() {
    ScreenshotTestTheme {
        ButlerCenteredTopAppBar(
            title = "Detail Screen",
            onBack = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TopAppBarWithAiActionPreview() {
    ScreenshotTestTheme {
        CompositionLocalProvider(LocalAiAvailable provides true) {
            ButlerCenteredTopAppBar(
                title = "AI Enabled",
                onBack = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TopAppBarWithMenuActionsPreview() {
    ScreenshotTestTheme {
        ButlerCenteredTopAppBar(
            title = "With Actions",
            actions = {
                IconButton(onClick = {}) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            },
        )
    }
}
