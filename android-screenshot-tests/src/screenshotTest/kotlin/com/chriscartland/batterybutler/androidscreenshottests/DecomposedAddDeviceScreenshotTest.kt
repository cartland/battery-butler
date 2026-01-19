package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.domain.ai.AiMessage
import com.chriscartland.batterybutler.domain.ai.AiRole
import com.chriscartland.batterybutler.presentationfeature.adddevice.AddDeviceAiSection
import com.chriscartland.batterybutler.presentationfeature.adddevice.AddDeviceManualSection
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(showBackground = true)
@Composable
fun AddDeviceAiSectionPreview() {
    AddDeviceAiSection(
        aiMessages = listOf(
            AiMessage("1", AiRole.USER, "Example prompt"),
            AiMessage("2", AiRole.MODEL, "Example response")
        ),
        onBatchAdd = {},
    )
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(showBackground = true)
@Composable
fun AddDeviceManualSectionPreview() {
    AddDeviceManualSection(
        name = "My Device",
        onNameChange = {},
        location = "Kitchen",
        onLocationChange = {},
        deviceTypes = listOf(fakeDeviceType),
        selectedType = fakeDeviceType,
        onTypeSelected = {},
        onManageDeviceTypesClick = {},
    )
}
