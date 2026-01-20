package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.androidscreenshottests.util.ScreenshotTestTheme
import com.chriscartland.batterybutler.presentationfeature.home.HomeScreenFilterRowPreview
import com.chriscartland.batterybutler.presentationfeature.home.HomeScreenListPreview
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(showBackground = true)
@Composable
fun HomeScreenFilterRowPreviewTest() {
    ScreenshotTestTheme {
        HomeScreenFilterRowPreview()
    }
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(showBackground = true)
@Composable
fun HomeScreenListPreviewTest() {
    ScreenshotTestTheme {
        HomeScreenListPreview()
    }
}
