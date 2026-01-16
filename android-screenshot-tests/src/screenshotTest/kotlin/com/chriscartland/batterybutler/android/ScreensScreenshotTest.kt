package com.chriscartland.batterybutler.android

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.domain.ai.AiMessage
import com.chriscartland.batterybutler.domain.ai.AiRole
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentation.feature.addbatteryevent.AddBatteryEventContent
import com.chriscartland.batterybutler.presentation.feature.adddevicetype.AddDeviceTypeContent
import com.chriscartland.batterybutler.presentation.feature.devicedetail.DeviceDetailContent
import com.chriscartland.batterybutler.presentation.feature.devicetypes.DeviceTypeListContent
import com.chriscartland.batterybutler.presentation.feature.devicetypes.EditDeviceTypeContent
import com.chriscartland.batterybutler.presentation.feature.editdevice.EditDeviceContent
import com.chriscartland.batterybutler.presentation.feature.eventdetail.EventDetailContent
import com.chriscartland.batterybutler.presentation.feature.history.HistoryListContent
import com.chriscartland.batterybutler.presentation.feature.home.HomeScreenContent
import com.chriscartland.batterybutler.presentation.feature.settings.SettingsContent
import com.chriscartland.batterybutler.presentation.models.devicedetail.DeviceDetailUiState
import com.chriscartland.batterybutler.presentation.models.devicetypes.DeviceTypeGroupOption
import com.chriscartland.batterybutler.presentation.models.devicetypes.DeviceTypeListUiState
import com.chriscartland.batterybutler.presentation.models.devicetypes.DeviceTypeSortOption
import com.chriscartland.batterybutler.presentation.models.devicetypes.EditDeviceTypeUiState
import com.chriscartland.batterybutler.presentation.models.editdevice.EditDeviceUiState
import com.chriscartland.batterybutler.presentation.models.eventdetail.EventDetailUiState
import com.chriscartland.batterybutler.presentation.models.history.HistoryListUiState
import com.chriscartland.batterybutler.presentation.models.home.HomeUiState
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
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun HistoryListScreenPreview() {
    val historyItem = com.chriscartland.batterybutler.presentation.models.history.HistoryItemUiModel(
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
