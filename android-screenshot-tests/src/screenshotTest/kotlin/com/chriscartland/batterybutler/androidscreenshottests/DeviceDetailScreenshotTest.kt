package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.androidscreenshottests.util.ScreenshotTestTheme
import com.chriscartland.batterybutler.presentationfeature.devicedetail.DeviceDetailContentPreview
import com.chriscartland.batterybutler.presentationfeature.devicedetail.DeviceDetailErrorPreview
import com.chriscartland.batterybutler.presentationfeature.devicedetail.DeviceDetailLoadingPreview
import com.chriscartland.batterybutler.presentationfeature.devicedetail.DeviceDetailNotFoundPreview

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DeviceDetailSuccessPreviewTest() {
    ScreenshotTestTheme {
        DeviceDetailContentPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DeviceDetailLoadingPreviewTest() {
    ScreenshotTestTheme {
        DeviceDetailLoadingPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DeviceDetailNotFoundPreviewTest() {
    ScreenshotTestTheme {
        DeviceDetailNotFoundPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DeviceDetailErrorPreviewTest() {
    ScreenshotTestTheme {
        DeviceDetailErrorPreview()
    }
}
