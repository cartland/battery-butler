package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.androidscreenshottests.util.ScreenshotTestTheme
import com.chriscartland.batterybutler.presentationfeature.main.DevicesScreenPreview
import com.chriscartland.batterybutler.presentationfeature.main.HistoryScreenPreview
import com.chriscartland.batterybutler.presentationfeature.main.TypesScreenPreview
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(device = "id:pixel_5", showBackground = true)
@Composable
fun DevicesScreenPreviewTest() {
    ScreenshotTestTheme {
        DevicesScreenPreview()
    }
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(device = "id:pixel_5", showBackground = true)
@Composable
fun TypesScreenPreviewTest() {
    ScreenshotTestTheme {
        TypesScreenPreview()
    }
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(device = "id:pixel_5", showBackground = true)
@Composable
fun HistoryScreenPreviewTest() {
    ScreenshotTestTheme {
        HistoryScreenPreview()
    }
}
