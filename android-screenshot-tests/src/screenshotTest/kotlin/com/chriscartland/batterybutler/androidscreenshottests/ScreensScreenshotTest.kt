package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.androidscreenshottests.util.ScreenshotTestTheme
import com.chriscartland.batterybutler.presentationfeature.addbatteryevent.AddBatteryEventContentPreview
import com.chriscartland.batterybutler.presentationfeature.adddevicetype.AddDeviceTypeContentPreview
import com.chriscartland.batterybutler.presentationfeature.devicedetail.DeviceDetailContentPreview
import com.chriscartland.batterybutler.presentationfeature.devicetypes.DeviceTypeListContentPreview
import com.chriscartland.batterybutler.presentationfeature.devicetypes.EditDeviceTypeContentPreview
import com.chriscartland.batterybutler.presentationfeature.editdevice.EditDeviceContentPreview
import com.chriscartland.batterybutler.presentationfeature.eventdetail.EventDetailContentPreview
import com.chriscartland.batterybutler.presentationfeature.history.HistoryListContentPreview
import com.chriscartland.batterybutler.presentationfeature.home.HomeScreenPreview
import com.chriscartland.batterybutler.presentationfeature.settings.SettingsContentPreview
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(showBackground = true)
@Composable
fun HomeScreenPreviewTest() {
    ScreenshotTestTheme {
        HomeScreenPreview()
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun DeviceTypeListScreenPreviewTest() {
    ScreenshotTestTheme {
        DeviceTypeListContentPreview()
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun HistoryListScreenPreviewTest() {
    ScreenshotTestTheme {
        HistoryListContentPreview()
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun AddDeviceTypeScreenPreviewTest() {
    ScreenshotTestTheme {
        AddDeviceTypeContentPreview()
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun AddBatteryEventScreenPreviewTest() {
    ScreenshotTestTheme {
        AddBatteryEventContentPreview()
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun DeviceDetailScreenPreviewTest() {
    ScreenshotTestTheme {
        DeviceDetailContentPreview()
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun EditDeviceScreenPreviewTest() {
    ScreenshotTestTheme {
        EditDeviceContentPreview()
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun EditDeviceTypeScreenPreviewTest() {
    ScreenshotTestTheme {
        EditDeviceTypeContentPreview()
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun EventDetailScreenPreviewTest() {
    ScreenshotTestTheme {
        EventDetailContentPreview()
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SettingsScreenPreviewTest() {
    ScreenshotTestTheme {
        SettingsContentPreview()
    }
}
