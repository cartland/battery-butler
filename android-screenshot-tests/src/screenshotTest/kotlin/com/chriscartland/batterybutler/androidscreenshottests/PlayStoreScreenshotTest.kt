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

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(device = "id:pixel_5", showBackground = true, name = "Light")
@Composable
fun PlayStoreHomeScreenTest() {
    ScreenshotTestTheme {
        HomeScreenPreview()
    }
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(device = "id:pixel_5", showBackground = true, name = "Light")
@Composable
fun PlayStoreDeviceDetailTest() {
    ScreenshotTestTheme {
        DeviceDetailContentPreview()
    }
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(device = "id:pixel_5", showBackground = true, name = "Light")
@Composable
fun PlayStoreAddDeviceTest() {
    ScreenshotTestTheme {
        AddDeviceContentPreview()
    }
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(device = "id:pixel_5", showBackground = true, name = "Light")
@Composable
fun PlayStoreHistoryTest() {
    ScreenshotTestTheme {
        HistoryListContentPreview()
    }
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(device = "id:pixel_5", showBackground = true, name = "Light")
@Composable
fun PlayStoreSettingsTest() {
    ScreenshotTestTheme {
        SettingsContentPreview()
    }
}
