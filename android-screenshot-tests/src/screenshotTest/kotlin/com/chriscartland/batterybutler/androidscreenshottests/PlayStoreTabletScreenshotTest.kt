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

private const val TABLET_7_INCH = "spec:width=1200px,height=1920px,dpi=213"

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(device = TABLET_7_INCH, showBackground = true, name = "Light")
@Composable
fun TabletHomeScreenTest() {
    ScreenshotTestTheme {
        HomeScreenPreview()
    }
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(device = TABLET_7_INCH, showBackground = true, name = "Light")
@Composable
fun TabletDeviceDetailTest() {
    ScreenshotTestTheme {
        DeviceDetailContentPreview()
    }
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(device = TABLET_7_INCH, showBackground = true, name = "Light")
@Composable
fun TabletAddDeviceTest() {
    ScreenshotTestTheme {
        AddDeviceContentPreview()
    }
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(device = TABLET_7_INCH, showBackground = true, name = "Light")
@Composable
fun TabletHistoryTest() {
    ScreenshotTestTheme {
        HistoryListContentPreview()
    }
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(device = TABLET_7_INCH, showBackground = true, name = "Light")
@Composable
fun TabletSettingsTest() {
    ScreenshotTestTheme {
        SettingsContentPreview()
    }
}
