package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.androidscreenshottests.util.ScreenshotTestTheme
import com.chriscartland.batterybutler.presentationfeature.addbatteryevent.AddBatteryEventContentPreview
import com.chriscartland.batterybutler.presentationfeature.adddevicetype.AddDeviceTypeContentPreview
import com.chriscartland.batterybutler.presentationfeature.aichat.AiChatContentEmptyPreview
import com.chriscartland.batterybutler.presentationfeature.aichat.AiChatContentPreview
import com.chriscartland.batterybutler.presentationfeature.devicetypes.DeviceTypeListContentEmptyPreview
import com.chriscartland.batterybutler.presentationfeature.devicetypes.EditDeviceTypeContentPreview
import com.chriscartland.batterybutler.presentationfeature.editdevice.EditDeviceContentPreview
import com.chriscartland.batterybutler.presentationfeature.history.HistoryListContentEmptyPreview
import com.chriscartland.batterybutler.presentationfeature.history.HistoryListContentLoadingPreview
import com.chriscartland.batterybutler.presentationfeature.home.HomeScreenEmptyPreview
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HomeScreenEmptyPreviewTest() {
    ScreenshotTestTheme {
        HomeScreenEmptyPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DeviceTypeListEmptyScreenPreviewTest() {
    ScreenshotTestTheme {
        DeviceTypeListContentEmptyPreview()
    }
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HistoryListEmptyScreenPreviewTest() {
    ScreenshotTestTheme {
        HistoryListContentEmptyPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddDeviceTypeScreenPreviewTest() {
    ScreenshotTestTheme {
        AddDeviceTypeContentPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddBatteryEventScreenPreviewTest() {
    ScreenshotTestTheme {
        AddBatteryEventContentPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EditDeviceScreenPreviewTest() {
    ScreenshotTestTheme {
        EditDeviceContentPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EditDeviceTypeScreenPreviewTest() {
    ScreenshotTestTheme {
        EditDeviceTypeContentPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AiChatContentPreviewTest() {
    ScreenshotTestTheme {
        AiChatContentPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AiChatContentEmptyPreviewTest() {
    ScreenshotTestTheme {
        AiChatContentEmptyPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HistoryListLoadingPreviewTest() {
    ScreenshotTestTheme {
        HistoryListContentLoadingPreview()
    }
}
