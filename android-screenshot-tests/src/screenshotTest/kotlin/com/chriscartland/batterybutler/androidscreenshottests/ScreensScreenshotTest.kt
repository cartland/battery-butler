package com.chriscartland.batterybutler.androidscreenshottests

import android.content.res.Configuration
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
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HomeScreenPreviewTest() {
    ScreenshotTestTheme {
        HomeScreenPreview()
    }
}

@PreviewTest
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DeviceTypeListScreenPreviewTest() {
    ScreenshotTestTheme {
        DeviceTypeListContentPreview()
    }
}

@PreviewTest
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HistoryListScreenPreviewTest() {
    ScreenshotTestTheme {
        HistoryListContentPreview()
    }
}

@PreviewTest
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddDeviceTypeScreenPreviewTest() {
    ScreenshotTestTheme {
        AddDeviceTypeContentPreview()
    }
}

@PreviewTest
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddBatteryEventScreenPreviewTest() {
    ScreenshotTestTheme {
        AddBatteryEventContentPreview()
    }
}

@PreviewTest
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DeviceDetailScreenPreviewTest() {
    ScreenshotTestTheme {
        DeviceDetailContentPreview()
    }
}

@PreviewTest
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EditDeviceScreenPreviewTest() {
    ScreenshotTestTheme {
        EditDeviceContentPreview()
    }
}

@PreviewTest
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EditDeviceTypeScreenPreviewTest() {
    ScreenshotTestTheme {
        EditDeviceTypeContentPreview()
    }
}

@PreviewTest
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EventDetailScreenPreviewTest() {
    ScreenshotTestTheme {
        EventDetailContentPreview()
    }
}

@PreviewTest
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsScreenPreviewTest() {
    ScreenshotTestTheme {
        SettingsContentPreview()
    }
}
