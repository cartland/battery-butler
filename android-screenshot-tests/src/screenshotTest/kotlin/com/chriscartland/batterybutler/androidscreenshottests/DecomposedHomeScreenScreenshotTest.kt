package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.presentationfeature.home.HomeScreenFilterRowPreview
import com.chriscartland.batterybutler.presentationfeature.home.HomeScreenListPreview
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(showBackground = true)
@Composable
fun HomeScreenFilterRowPreviewTest() {
    HomeScreenFilterRowPreview()
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(showBackground = true)
@Composable
fun HomeScreenListPreviewTest() {
    HomeScreenListPreview()
}
