package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.presentationcore.components.CompositeControlPreview
import com.chriscartland.batterybutler.presentationcore.components.DeviceListItemPreview
import com.chriscartland.batterybutler.presentationcore.components.HistoryListItemPreview

@PreviewTest
@Preview(showBackground = true)
@Composable
fun CompositeControlPreviewTest() {
    CompositeControlPreview()
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun HistoryListItemPreviewTest() {
    HistoryListItemPreview()
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun DeviceListItemPreviewTest() {
    DeviceListItemPreview()
}
