package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.presentationfeature.addbatteryevent.AddBatteryEventContentPreview
import com.chriscartland.batterybutler.presentationfeature.adddevicetype.AddDeviceTypeContentPreview
import com.chriscartland.batterybutler.presentationfeature.devicedetail.DeviceDetailContentPreview
import com.chriscartland.batterybutler.presentationfeature.devicetypes.DeviceTypeListContentPreview
import com.chriscartland.batterybutler.presentationfeature.devicetypes.EditDeviceTypeContentPreview
import com.chriscartland.batterybutler.presentationfeature.editdevice.EditDeviceContentPreview
import com.chriscartland.batterybutler.presentationfeature.eventdetail.EventDetailContentPreview
import com.chriscartland.batterybutler.presentationfeature.history.HistoryListContentPreview
import com.chriscartland.batterybutler.presentationfeature.home.HomeScreenPreview
import com.chriscartland.batterybutler.presentationfeature.settings.SettingsContentPreview
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// Shared Fakes
// Shared Fakes
@OptIn(ExperimentalTime::class) // For Clock.System.now() if needed, though kotlin.time.Clock is stable since 1.9, verify env
val fakeDeviceType = DeviceType("type1", "Smoke Detector", "detector_smoke")
val fakeDevice = Device(
    id = "dev1",
    name = "Kitchen Smoke Alarm",
    typeId = "type1",
    batteryLastReplaced = Instant.parse("2026-01-18T17:00:00Z"),
    lastUpdated = Instant.parse("2026-01-18T17:00:00Z"),
    location = "Kitchen",
)
val fakeEvent = BatteryEvent(
    id = "evt1",
    deviceId = "dev1",
    date = Instant.parse("2026-01-18T17:00:00Z"),
)

// region Test Shell
enum class TestMainTab {
    Devices,
    Types,
    History,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestMainScreenShell(
    currentTab: TestMainTab,
    title: String,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            ButlerCenteredTopAppBar(
                title = title,
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {}) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        },
        bottomBar = {
            NavigationBar {
                TestMainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = {},
                        icon = {
                            val icon = when (tab) {
                                TestMainTab.Devices -> Icons.Filled.Home
                                TestMainTab.Types -> Icons.AutoMirrored.Filled.List
                                TestMainTab.History -> Icons.Filled.History
                            }
                            Icon(icon, contentDescription = tab.name)
                        },
                        label = { Text(tab.name) },
                    )
                }
            }
        },
        content = content,
    )
}
// endregion

@PreviewTest
@Preview(showBackground = true)
@Composable
fun HomeScreenPreviewTest() {
    HomeScreenPreview()
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun DeviceTypeListScreenPreviewTest() {
    DeviceTypeListContentPreview()
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun HistoryListScreenPreviewTest() {
    HistoryListContentPreview()
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun AddDeviceTypeScreenPreviewTest() {
    AddDeviceTypeContentPreview()
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun AddBatteryEventScreenPreviewTest() {
    AddBatteryEventContentPreview()
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun DeviceDetailScreenPreviewTest() {
    DeviceDetailContentPreview()
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun EditDeviceScreenPreviewTest() {
    EditDeviceContentPreview()
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun EditDeviceTypeScreenPreviewTest() {
    EditDeviceTypeContentPreview()
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun EventDetailScreenPreviewTest() {
    EventDetailContentPreview()
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SettingsScreenPreviewTest() {
    SettingsContentPreview()
}
