package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.androidscreenshottests.util.ScreenshotTestTheme
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBarPreview
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBarWithActionsPreview
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBarWithAiPreview

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
fun TopAppBarWithAiActionPreviewTest() {
    ScreenshotTestTheme {
        ButlerCenteredTopAppBarWithAiPreview()
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TopAppBarWithMenuActionsPreviewTest() {
    ScreenshotTestTheme {
        ButlerCenteredTopAppBarWithActionsPreview()
    }
}
