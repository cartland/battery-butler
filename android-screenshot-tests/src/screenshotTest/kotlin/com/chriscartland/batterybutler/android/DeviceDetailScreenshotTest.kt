package com.chriscartland.batterybutler.android

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presenter.feature.devicedetail.DeviceDetailContent
import com.chriscartland.batterybutler.presenter.feature.devicedetail.DeviceDetailUiState
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days

@PreviewTest
@Preview(showBackground = true)
@Composable
fun DeviceDetailSuccessPreview() {
    val now = Clock.System.now()
    val device = Device(
        id = "1",
        name = "Living Room Smoke Detector",
        typeId = "type1",
        location = "Living Room",
        batteryLastReplaced = now.minus(180.days),
        lastUpdated = now,
    )
    val type = DeviceType(
        id = "type1",
        name = "Smoke Detector",
        defaultIcon = "detector_smoke",
        batteryType = "9V",
        batteryQuantity = 1,
    )
    val events = listOf(
        BatteryEvent("e1", "1", now.minus(180.days), "Replaced battery"),
        BatteryEvent("e2", "1", now.minus(365.days), "Initial setup"),
    )

    DeviceDetailContent(
        state = DeviceDetailUiState.Success(device, type, events),
        onRecordReplacement = {},
        onBack = {},
        onEdit = {},
        onEventClick = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun DeviceDetailLoadingPreview() {
    DeviceDetailContent(
        state = DeviceDetailUiState.Loading,
        onRecordReplacement = {},
        onBack = {},
        onEdit = {},
        onEventClick = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun DeviceDetailNotFoundPreview() {
    DeviceDetailContent(
        state = DeviceDetailUiState.NotFound,
        onRecordReplacement = {},
        onBack = {},
        onEdit = {},
        onEventClick = {},
    )
}
