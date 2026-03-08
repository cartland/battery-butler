package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.androidscreenshottests.util.ScreenshotTestTheme
import com.chriscartland.batterybutler.presentationfeature.addbatteryevent.AddBatteryEventContentEmptyPreview
import com.chriscartland.batterybutler.presentationfeature.devicetypes.DeviceTypeListContentLoadingPreview
import com.chriscartland.batterybutler.presentationfeature.devicetypes.EditDeviceTypeLoadingPreview
import com.chriscartland.batterybutler.presentationfeature.devicetypes.EditDeviceTypeNotFoundPreview
import com.chriscartland.batterybutler.presentationfeature.editdevice.EditDeviceLoadingPreview
import com.chriscartland.batterybutler.presentationfeature.editdevice.EditDeviceNotFoundPreview
import com.chriscartland.batterybutler.presentationfeature.eventdetail.EventDetailLoadingPreview

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DeviceTypeListLoadingPreviewTest() {
    ScreenshotTestTheme {
        DeviceTypeListContentLoadingPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EventDetailLoadingPreviewTest() {
    ScreenshotTestTheme {
        EventDetailLoadingPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EditDeviceTypeLoadingPreviewTest() {
    ScreenshotTestTheme {
        EditDeviceTypeLoadingPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EditDeviceTypeNotFoundPreviewTest() {
    ScreenshotTestTheme {
        EditDeviceTypeNotFoundPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EditDeviceLoadingPreviewTest() {
    ScreenshotTestTheme {
        EditDeviceLoadingPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EditDeviceNotFoundPreviewTest() {
    ScreenshotTestTheme {
        EditDeviceNotFoundPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddBatteryEventEmptyPreviewTest() {
    ScreenshotTestTheme {
        AddBatteryEventContentEmptyPreview()
    }
}
