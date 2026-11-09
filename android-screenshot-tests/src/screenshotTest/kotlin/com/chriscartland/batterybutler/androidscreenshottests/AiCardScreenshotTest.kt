package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.androidscreenshottests.util.ScreenshotTestTheme
import com.chriscartland.batterybutler.presentationfeature.aichat.AiTabContentCardStatesPreview

@PreviewTest
@Preview(device = "spec:width=393dp,height=1500dp", showBackground = true, name = "Light")
@Preview(
    device = "spec:width=393dp,height=1500dp",
    showBackground = true,
    name = "Dark",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun AiCardStatesPreviewTest() {
    ScreenshotTestTheme {
        AiTabContentCardStatesPreview()
    }
}
