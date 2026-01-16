package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.domain.ai.AiMessage
import com.chriscartland.batterybutler.domain.ai.AiRole
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationfeature.adddevice.AddDeviceContent

@PreviewTest
@Preview(showBackground = true)
@Composable
fun AddDeviceEmptyPreview() {
    AddDeviceContent(
        deviceTypes = emptyList(),
        aiMessages = emptyList(),
        onAddDevice = {},
        onBatchAdd = {},
        onManageDeviceTypesClick = {},
        onBack = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun AddDeviceWithTypesAndAiPreview() {
    val types = listOf(
        DeviceType("1", "Smoke Detector", "detector_smoke"),
        DeviceType("2", "Smart Lock", "lock"),
    )
    val messages = listOf(
        AiMessage("1", AiRole.USER, "Add a smoke detector in the kitchen"),
        AiMessage("2", AiRole.MODEL, "I've detected a Smoke Detector at location 'Kitchen'. Please confirm."),
    )

    AddDeviceContent(
        deviceTypes = types,
        aiMessages = messages,
        onAddDevice = {},
        onBatchAdd = {},
        onManageDeviceTypesClick = {},
        onBack = {},
    )
}
