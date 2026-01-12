package com.chriscartland.batterybutler.android

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.domain.ai.AiMessage
import com.chriscartland.batterybutler.domain.ai.AiRole
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.ui.feature.addbatteryevent.AddBatteryEventContent
import com.chriscartland.batterybutler.ui.feature.adddevicetype.AddDeviceTypeContent
import com.chriscartland.batterybutler.ui.feature.devicedetail.DeviceDetailContent
import com.chriscartland.batterybutler.ui.feature.devicedetail.DeviceDetailUiState
import com.chriscartland.batterybutler.ui.feature.devicetypes.DeviceTypeListContent
import com.chriscartland.batterybutler.ui.feature.devicetypes.DeviceTypeListUiState
import com.chriscartland.batterybutler.ui.feature.devicetypes.EditDeviceTypeContent
import com.chriscartland.batterybutler.ui.feature.devicetypes.EditDeviceTypeUiState
import com.chriscartland.batterybutler.ui.feature.devicetypes.UiDeviceTypeGroupOption
import com.chriscartland.batterybutler.ui.feature.devicetypes.UiDeviceTypeSortOption
import com.chriscartland.batterybutler.ui.feature.editdevice.EditDeviceContent
import com.chriscartland.batterybutler.ui.feature.editdevice.EditDeviceUiState
import com.chriscartland.batterybutler.ui.feature.eventdetail.EventDetailContent
import com.chriscartland.batterybutler.ui.feature.history.HistoryListContent
import com.chriscartland.batterybutler.ui.feature.home.HomeScreenContent
import com.chriscartland.batterybutler.ui.feature.settings.SettingsContent
import com.chriscartland.batterybutler.viewmodel.eventdetail.EventDetailUiState
import com.chriscartland.batterybutler.viewmodel.history.HistoryListUiState
import com.chriscartland.batterybutler.viewmodel.home.HomeUiState
import kotlinx.datetime.Clock

// Shared Fakes
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

@PreviewTest
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
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
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun DeviceTypeListScreenPreview() {
    DeviceTypeListContent(
        state = DeviceTypeListUiState.Success(
            groupedTypes = mapOf("All" to listOf(fakeDeviceType)),
            sortOption = UiDeviceTypeSortOption.NAME,
            groupOption = UiDeviceTypeGroupOption.NONE,
            isSortAscending = true,
            isGroupAscending = true,
        ),
        onEditType = {},
        onSortDirectionToggle = {},
        onGroupDirectionToggle = {},
        onSortOptionSelected = {},
        onGroupOptionSelected = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun HistoryListScreenPreview() {
    val historyItem = com.chriscartland.batterybutler.viewmodel.history.HistoryItemUiModel(
        event = fakeEvent,
        deviceName = fakeDevice.name,
        deviceTypeName = fakeDeviceType.name,
        deviceLocation = "Kitchen",
    )
    HistoryListContent(
        state = HistoryListUiState.Success(
            items = listOf(historyItem),
        ),
        onEventClick = { _, _ -> },
    )
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
