package com.chriscartland.batterybutler.android

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.feature.home.HomeScreenContent
import com.chriscartland.batterybutler.viewmodel.home.GroupOption
import com.chriscartland.batterybutler.viewmodel.home.HomeUiState
import com.chriscartland.batterybutler.viewmodel.home.SortOption
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days

@PreviewTest
@Preview(showBackground = true)
@Composable
fun HomeScreenContentGroupedPreview() {
    val now = Clock.System.now()
    val typeLight = DeviceType(id = "light", name = "Light", defaultIcon = "lightbulb")
    val typeSensor = DeviceType(id = "sensor", name = "Sensor", defaultIcon = "sensor_door")

    val devices = listOf(
        Device("1", "Living Room Light", "light", now.minus(50.days), now, location = "Living Room"),
        Device("2", "Kitchen Light", "light", now.minus(20.days), now, location = "Kitchen"),
        Device("3", "Front Door", "sensor", now.minus(100.days), now, location = "Entrance"),
    )

    val grouped = mapOf(
        "Light" to listOf(devices[0], devices[1]),
        "Sensor" to listOf(devices[2]),
    )

    val state = HomeUiState(
        groupedDevices = grouped,
        deviceTypes = mapOf("light" to typeLight, "sensor" to typeSensor),
        sortOption = SortOption.NAME,
        groupOption = GroupOption.TYPE,
        isSortAscending = true,
        isGroupAscending = true,
    )

    HomeScreenContent(
        state = state,
        onGroupOptionToggle = {},
        onGroupOptionSelected = {},
        onSortOptionToggle = {},
        onSortOptionSelected = {},
        onDeviceClick = {},
    )
}
