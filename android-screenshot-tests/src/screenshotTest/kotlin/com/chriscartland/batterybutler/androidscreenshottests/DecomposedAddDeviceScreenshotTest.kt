package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.androidscreenshottests.util.ScreenshotTestTheme
import com.chriscartland.batterybutler.presentationfeature.adddevice.AddDeviceAiSectionPreview
import com.chriscartland.batterybutler.presentationfeature.adddevice.AddDeviceManualSectionPreview
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddDeviceAiSectionPreviewTest() {
    ScreenshotTestTheme {
        AddDeviceAiSectionPreview()
    }
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddDeviceManualSectionPreviewTest() {
    ScreenshotTestTheme {
        AddDeviceManualSectionPreview()
    }
}
