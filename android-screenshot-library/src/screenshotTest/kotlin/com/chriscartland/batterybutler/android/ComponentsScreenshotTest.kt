package com.chriscartland.batterybutler.android

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.ui.components.CompositeControl
import com.chriscartland.batterybutler.ui.components.DeviceListItem
import com.chriscartland.batterybutler.ui.components.HistoryListItem
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

@PreviewTest
@Preview(showBackground = true)
@Composable
fun CompositeControlAscendingPreview() {
    CompositeControl(
        label = "Battery Level",
        isActive = true,
        isAscending = true,
        onClicked = {},
        onDirectionToggle = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun CompositeControlDescendingPreview() {
    CompositeControl(
        label = "Date Added",
        isActive = true,
        isAscending = false,
        onClicked = {},
        onDirectionToggle = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun CompositeControlInactivePreview() {
    CompositeControl(
        label = "Name",
        isActive = false,
        isAscending = true, // Should be ignored visually if inactive/implementation dependent
        onClicked = {},
        onDirectionToggle = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun HistoryListItemPreview() {
    val now = Clock.System.now()
    val event = BatteryEvent(
        id = "1",
        deviceId = "device1",
        date = now,
        notes = "Replaced battery",
    )
    HistoryListItem(
        event = event,
        deviceName = "Smoke Detector",
        deviceTypeName = "Sensor",
        deviceLocation = "Kitchen",
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun HistoryListItemOldPreview() {
    val now = Clock.System.now()
    val event = BatteryEvent(
        id = "2",
        deviceId = "device2",
        date = now.minus(30.days),
        notes = "Low battery warning",
    )
    HistoryListItem(
        event = event,
        deviceName = "Smart Lock",
        deviceTypeName = "Security",
        deviceLocation = "Front Door",
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun DeviceListItemPreview() {
    val device = Device(
        id = "d1",
        name = "Living Room Lamp",
        typeId = "type1",
        location = "Living Room",
        batteryLastReplaced = Clock.System.now().minus(10.days),
        lastUpdated = Clock.System.now(),
    )
    val type = DeviceType(
        id = "type1",
        name = "Light",
        defaultIcon = "lightbulb",
    )
    DeviceListItem(
        device = device,
        deviceType = type,
        onClick = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun DeviceListItemNoLocationPreview() {
    val device = Device(
        id = "d2",
        name = "Unknown Device",
        typeId = "type2",
        location = "",
        batteryLastReplaced = Instant.fromEpochMilliseconds(0), // Never replaced
        lastUpdated = Clock.System.now(),
    )
    val type = DeviceType(
        id = "type2",
        name = "Gadget",
        defaultIcon = "devices",
    )
    DeviceListItem(
        device = device,
        deviceType = type,
        onClick = {},
    )
}
