package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.presentationfeature.adddevice.AddDeviceContentPreview

@PreviewTest
@Preview(showBackground = true)
@Composable
fun AddDeviceEmptyPreviewTest() {
    AddDeviceContentPreview()
}
