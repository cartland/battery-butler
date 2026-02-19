package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.androidscreenshottests.util.ScreenshotTestTheme
import com.chriscartland.batterybutler.presentationfeature.adddevice.AddDeviceContentPreview
import com.chriscartland.batterybutler.presentationfeature.devicedetail.DeviceDetailContentPreview
import com.chriscartland.batterybutler.presentationfeature.history.HistoryListContentPreview
import com.chriscartland.batterybutler.presentationfeature.home.HomeScreenPreview
import com.chriscartland.batterybutler.presentationfeature.settings.SettingsContentPreview
import kotlin.time.ExperimentalTime

private const val TABLET_10_INCH = "spec:width=1600px,height=2560px,dpi=320"

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(device = TABLET_10_INCH, showBackground = true, name = "Light")
@Composable
fun Tablet10HomeScreenTest() {
    ScreenshotTestTheme {
        HomeScreenPreview()
    }
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(device = TABLET_10_INCH, showBackground = true, name = "Light")
@Composable
fun Tablet10DeviceDetailTest() {
    ScreenshotTestTheme {
        DeviceDetailContentPreview()
    }
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(device = TABLET_10_INCH, showBackground = true, name = "Light")
@Composable
fun Tablet10AddDeviceTest() {
    ScreenshotTestTheme {
        AddDeviceContentPreview()
    }
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(device = TABLET_10_INCH, showBackground = true, name = "Light")
@Composable
fun Tablet10HistoryTest() {
    ScreenshotTestTheme {
        HistoryListContentPreview()
    }
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(device = TABLET_10_INCH, showBackground = true, name = "Light")
@Composable
fun Tablet10SettingsTest() {
    ScreenshotTestTheme {
        SettingsContentPreview()
    }
}
