package com.chriscartland.batterybutler.androidscreenshottests

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.androidscreenshottests.util.ScreenshotTestTheme
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBarPreview
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBarWithActionsPreview
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBarWithAiPreview

@PreviewTest
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TopAppBarDefaultPreviewTest() {
    ScreenshotTestTheme {
        ButlerCenteredTopAppBarPreview()
    }
}

@PreviewTest
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TopAppBarWithAiActionPreviewTest() {
    ScreenshotTestTheme {
        ButlerCenteredTopAppBarWithAiPreview()
    }
}

@PreviewTest
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TopAppBarWithMenuActionsPreviewTest() {
    ScreenshotTestTheme {
        ButlerCenteredTopAppBarWithActionsPreview()
    }
}
