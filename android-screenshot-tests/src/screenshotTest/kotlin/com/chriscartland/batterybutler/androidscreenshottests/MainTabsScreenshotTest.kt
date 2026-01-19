package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationfeature.main.DevicesScreenPreview
import com.chriscartland.batterybutler.presentationfeature.main.HistoryScreenPreview
import com.chriscartland.batterybutler.presentationfeature.main.TypesScreenPreview
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeListUiState
import com.chriscartland.batterybutler.presentationmodel.history.HistoryItemUiModel
import com.chriscartland.batterybutler.presentationmodel.history.HistoryListUiState
import com.chriscartland.batterybutler.presentationmodel.home.HomeUiState
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

// Local Fakes for this test file
private val fakeHistoryItem = HistoryItemUiModel(
    event = fakeEvent,
    deviceName = fakeDevice.name,
    deviceTypeName = fakeDeviceType.name,
    deviceLocation = "Kitchen",
)

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(device = "id:pixel_5", showBackground = true)
@Composable
fun DevicesScreenPreviewTest() {
    DevicesScreenPreview()
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(device = "id:pixel_5", showBackground = true)
@Composable
fun TypesScreenPreviewTest() {
    TypesScreenPreview()
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(device = "id:pixel_5", showBackground = true)
@Composable
fun HistoryScreenPreviewTest() {
    HistoryScreenPreview()
}
