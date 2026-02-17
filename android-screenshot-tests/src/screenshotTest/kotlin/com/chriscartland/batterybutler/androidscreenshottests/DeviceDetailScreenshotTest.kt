package com.chriscartland.batterybutler.androidscreenshottests

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.androidscreenshottests.util.ScreenshotTestTheme
import com.chriscartland.batterybutler.presentationfeature.devicedetail.DeviceDetailContentPreview
import com.chriscartland.batterybutler.presentationfeature.devicedetail.DeviceDetailLoadingPreview
import com.chriscartland.batterybutler.presentationfeature.devicedetail.DeviceDetailNotFoundPreview

@PreviewTest
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DeviceDetailSuccessPreviewTest() {
    ScreenshotTestTheme {
        DeviceDetailContentPreview()
    }
}

@PreviewTest
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DeviceDetailLoadingPreviewTest() {
    ScreenshotTestTheme {
        DeviceDetailLoadingPreview()
    }
}

@PreviewTest
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DeviceDetailNotFoundPreviewTest() {
    ScreenshotTestTheme {
        DeviceDetailNotFoundPreview()
    }
}
