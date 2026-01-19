package com.chriscartland.batterybutler.androidscreenshottests



import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.domain.ai.AiMessage
import com.chriscartland.batterybutler.domain.ai.AiRole
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationfeature.addbatteryevent.AddBatteryEventContent
import com.chriscartland.batterybutler.presentationfeature.adddevicetype.AddDeviceTypeContent
import com.chriscartland.batterybutler.presentationfeature.devicedetail.DeviceDetailContent
import com.chriscartland.batterybutler.presentationfeature.devicetypes.DeviceTypeListContent
import com.chriscartland.batterybutler.presentationfeature.devicetypes.EditDeviceTypeContent
import com.chriscartland.batterybutler.presentationfeature.editdevice.EditDeviceContent
import com.chriscartland.batterybutler.presentationfeature.eventdetail.EventDetailContent
import com.chriscartland.batterybutler.presentationfeature.history.HistoryListContent
import com.chriscartland.batterybutler.presentationfeature.home.HomeScreenContent
import com.chriscartland.batterybutler.presentationfeature.settings.SettingsContent
import com.chriscartland.batterybutler.presentationmodel.devicedetail.DeviceDetailUiState
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeGroupOption
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeListUiState
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeSortOption
import com.chriscartland.batterybutler.presentationmodel.devicetypes.EditDeviceTypeUiState
import com.chriscartland.batterybutler.presentationmodel.editdevice.EditDeviceUiState
import com.chriscartland.batterybutler.presentationmodel.eventdetail.EventDetailUiState
import com.chriscartland.batterybutler.presentationmodel.history.HistoryItemUiModel
import com.chriscartland.batterybutler.presentationmodel.history.HistoryListUiState
import com.chriscartland.batterybutler.presentationmodel.home.HomeUiState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBar
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

// Shared Fakes
// Shared Fakes
@OptIn(ExperimentalTime::class) // For Clock.System.now() if needed, though kotlin.time.Clock is stable since 1.9, verify env
val fakeDeviceType = DeviceType("type1", "Smoke Detector", "detector_smoke")
val fakeDevice = Device(
    id = "dev1",
    name = "Kitchen Smoke Alarm",
    typeId = "type1",
    batteryLastReplaced = Clock.System.now(),
    lastUpdated = Clock.System.now(),
    location = "Kitchen",
)
val fakeEvent = BatteryEvent(
    id = "evt1",
    deviceId = "dev1",
    date = Clock.System.now(),
)

// region Test Shell
enum class TestMainTab {
    Devices, Types, History
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestMainScreenShell(
    currentTab: TestMainTab,
    title: String,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            ButlerCenteredTopAppBar(
                title = title,
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
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
                        label = { Text(tab.name) }
                    )
                }
            }
        },
        content = content
    )
}
// endregion

@PreviewTest
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    TestMainScreenShell(TestMainTab.Devices, title = "Devices") { innerPadding ->
        HomeScreenContent(
            state = HomeUiState(
                groupedDevices = mapOf("All" to listOf(fakeDevice)),
                deviceTypes = mapOf(fakeDeviceType.id to fakeDeviceType),
            ),
            onGroupOptionToggle = {},
            onGroupOptionSelected = {},
            onSortOptionToggle = {},
            onSortOptionSelected = {},
            onDeviceClick = {},
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun DeviceTypeListScreenPreview() {
    TestMainScreenShell(TestMainTab.Types, title = "Device Types") { innerPadding ->
        DeviceTypeListContent(
            state = DeviceTypeListUiState.Success(
                groupedTypes = mapOf("All" to listOf(fakeDeviceType)),
                sortOption = DeviceTypeSortOption.NAME,
                groupOption = DeviceTypeGroupOption.NONE,
                isSortAscending = true,
                isGroupAscending = true,
            ),
            onEditType = {},
            onSortDirectionToggle = {},
            onGroupDirectionToggle = {},
            onSortOptionSelected = {},
            onGroupOptionSelected = {},
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun HistoryListScreenPreview() {
    val historyItem = HistoryItemUiModel(
        event = fakeEvent,
        deviceName = fakeDevice.name,
        deviceTypeName = fakeDeviceType.name,
        deviceLocation = "Kitchen",
    )
    TestMainScreenShell(TestMainTab.History, title = "History") { innerPadding ->
        HistoryListContent(
            state = HistoryListUiState.Success(
                items = listOf(historyItem),
            ),
            onEventClick = { _, _ -> },
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun AddDeviceTypeScreenPreview() {
    AddDeviceTypeContent(
        aiMessages = listOf(
            AiMessage("1", AiRole.USER, "Add Smoke Detector"),
            AiMessage("2", AiRole.MODEL, "Confirmed."),
        ),
        suggestedIcon = "detector_smoke",
        onSuggestIcon = {},
        onConsumeSuggestedIcon = {},
        onDeviceTypeAdded = {},
        onBatchAdd = {},
        onBack = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun AddBatteryEventScreenPreview() {
    AddBatteryEventContent(
        devices = listOf(fakeDevice),
        aiMessages = emptyList(),
        onAddEvent = { _, _ -> },
        onBatchAdd = {},
        onAddDeviceClick = {},
        onBack = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun DeviceDetailScreenPreview() {
    DeviceDetailContent(
        state = DeviceDetailUiState.Success(
            device = fakeDevice,
            deviceType = fakeDeviceType,
            events = listOf(fakeEvent),
        ),
        onRecordReplacement = {},
        onBack = {},
        onEdit = {},
        onEventClick = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun EditDeviceScreenPreview() {
    EditDeviceContent(
        uiState = EditDeviceUiState.Success(
            device = fakeDevice,
            deviceTypes = listOf(fakeDeviceType),
        ),
        onSave = {},
        onDelete = {},
        onManageDeviceTypesClick = {},
        onBack = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun EditDeviceTypeScreenPreview() {
    EditDeviceTypeContent(
        uiState = EditDeviceTypeUiState.Success(
            deviceType = fakeDeviceType,
        ),
        onSave = {},
        onDelete = {},
        onBack = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun EventDetailScreenPreview() {
    EventDetailContent(
        uiState = EventDetailUiState.Success(
            event = fakeEvent,
            device = fakeDevice,
            deviceType = fakeDeviceType,
        ),
        onUpdateDate = {},
        onDeleteEvent = {},
        onBack = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsContent(
        onExportData = {},
        onBack = {},
    )
}
