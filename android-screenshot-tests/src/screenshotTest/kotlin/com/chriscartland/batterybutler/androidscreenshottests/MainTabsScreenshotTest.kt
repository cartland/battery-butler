package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationfeature.main.DevicesScreen
import com.chriscartland.batterybutler.presentationfeature.main.HistoryScreen
import com.chriscartland.batterybutler.presentationfeature.main.TypesScreen
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
fun DevicesScreenPreview() {
    BatteryButlerTheme {
        DevicesScreen(
            state = HomeUiState(
                groupedDevices = mapOf("All" to listOf(fakeDevice)),
                deviceTypes = mapOf(fakeDeviceType.id to fakeDeviceType),
            ),
            onTabSelected = {},
            onSettingsClick = {},
            onAddDeviceClick = {},
            onDeviceClick = {},
            onGroupOptionToggle = {},
            onGroupOptionSelected = {},
            onSortOptionToggle = {},
            onSortOptionSelected = {},
        )
    }
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(device = "id:pixel_5", showBackground = true)
@Composable
fun TypesScreenPreview() {
    BatteryButlerTheme {
        TypesScreen(
            state = DeviceTypeListUiState.Success(
                groupedTypes = mapOf("All" to listOf(fakeDeviceType)),
            ),
            onTabSelected = {},
            onSettingsClick = {},
            onAddTypeClick = {},
            onEditType = {},
            onSortOptionSelected = {},
            onGroupOptionSelected = {},
            onSortDirectionToggle = {},
            onGroupDirectionToggle = {},
        )
    }
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(device = "id:pixel_5", showBackground = true)
@Composable
fun HistoryScreenPreview() {
    BatteryButlerTheme {
        HistoryScreen(
            state = HistoryListUiState.Success(
                items = listOf(fakeHistoryItem),
            ),
            onTabSelected = {},
            onSettingsClick = {},
            onAddEventClick = {},
            onEventClick = { _, _ -> },
        )
    }
}
