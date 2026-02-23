package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.androidscreenshottests.util.ScreenshotTestTheme
import com.chriscartland.batterybutler.presentationfeature.main.AiBarCollapsedDevicesPreview
import com.chriscartland.batterybutler.presentationfeature.main.AiBarCollapsedHistoryPreview
import com.chriscartland.batterybutler.presentationfeature.main.AiBarCollapsedTypesPreview
import com.chriscartland.batterybutler.presentationfeature.main.AiOverlayExpandedPreview
import kotlin.time.ExperimentalTime

@PreviewTest
@Preview(device = "id:pixel_5", showBackground = true, name = "Light")
@Preview(
    device = "id:pixel_5",
    showBackground = true,
    name = "Dark",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun AiBarCollapsedDevicesPreviewTest() {
    ScreenshotTestTheme {
        AiBarCollapsedDevicesPreview()
    }
}

@PreviewTest
@Preview(device = "id:pixel_5", showBackground = true, name = "Light")
@Preview(
    device = "id:pixel_5",
    showBackground = true,
    name = "Dark",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun AiBarCollapsedTypesPreviewTest() {
    ScreenshotTestTheme {
        AiBarCollapsedTypesPreview()
    }
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(device = "id:pixel_5", showBackground = true, name = "Light")
@Preview(
    device = "id:pixel_5",
    showBackground = true,
    name = "Dark",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun AiBarCollapsedHistoryPreviewTest() {
    ScreenshotTestTheme {
        AiBarCollapsedHistoryPreview()
    }
}

@PreviewTest
@Preview(device = "id:pixel_5", showBackground = true, name = "Light")
@Preview(
    device = "id:pixel_5",
    showBackground = true,
    name = "Dark",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun AiOverlayExpandedPreviewTest() {
    ScreenshotTestTheme {
        AiOverlayExpandedPreview()
    }
}
