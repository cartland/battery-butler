package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.domain.ai.AiMessage
import com.chriscartland.batterybutler.domain.ai.AiRole
import com.chriscartland.batterybutler.presentationfeature.adddevice.AddDeviceAiSectionPreview
import com.chriscartland.batterybutler.presentationfeature.adddevice.AddDeviceManualSectionPreview
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(showBackground = true)
@Composable
fun AddDeviceAiSectionPreviewTest() {
    AddDeviceAiSectionPreview()
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(showBackground = true)
@Composable
fun AddDeviceManualSectionPreviewTest() {
    AddDeviceManualSectionPreview()
}
