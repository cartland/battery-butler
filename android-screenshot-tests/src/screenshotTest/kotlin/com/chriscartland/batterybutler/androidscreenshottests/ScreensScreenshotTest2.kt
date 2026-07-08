package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.androidscreenshottests.util.ScreenshotTestTheme
import com.chriscartland.batterybutler.presentationfeature.devicetypes.DeviceTypeDetailContentPreview
import com.chriscartland.batterybutler.presentationfeature.devicetypes.DeviceTypeDetailLoadingPreview
import com.chriscartland.batterybutler.presentationfeature.devicetypes.DeviceTypeDetailNotFoundPreview
import com.chriscartland.batterybutler.presentationfeature.eventdetail.EditBatteryEventContentPreview
import com.chriscartland.batterybutler.presentationfeature.eventdetail.EditBatteryEventNotFoundPreview
import com.chriscartland.batterybutler.presentationfeature.eventdetail.EventDetailContentDeletedDevicePreview
import com.chriscartland.batterybutler.presentationfeature.eventdetail.EventDetailContentNotFoundPreview
import com.chriscartland.batterybutler.presentationfeature.eventdetail.EventDetailContentPreview
import com.chriscartland.batterybutler.presentationfeature.settings.SettingsContentAllDataModesPreview
import com.chriscartland.batterybutler.presentationfeature.settings.SettingsContentPreview
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EventDetailScreenPreviewTest() {
    ScreenshotTestTheme {
        EventDetailContentPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EventDetailDeletedDeviceScreenPreviewTest() {
    ScreenshotTestTheme {
        EventDetailContentDeletedDevicePreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsScreenPreviewTest() {
    ScreenshotTestTheme {
        SettingsContentPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsAllDataModesPreviewTest() {
    ScreenshotTestTheme {
        SettingsContentAllDataModesPreview()
    }
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DeviceTypeDetailScreenPreviewTest() {
    ScreenshotTestTheme {
        DeviceTypeDetailContentPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DeviceTypeDetailLoadingPreviewTest() {
    ScreenshotTestTheme {
        DeviceTypeDetailLoadingPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DeviceTypeDetailNotFoundPreviewTest() {
    ScreenshotTestTheme {
        DeviceTypeDetailNotFoundPreview()
    }
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EditBatteryEventScreenPreviewTest() {
    ScreenshotTestTheme {
        EditBatteryEventContentPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EditBatteryEventNotFoundPreviewTest() {
    ScreenshotTestTheme {
        EditBatteryEventNotFoundPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EventDetailNotFoundPreviewTest() {
    ScreenshotTestTheme {
        EventDetailContentNotFoundPreview()
    }
}
