package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.presentationfeature.home.HomeScreenFilterRow
import com.chriscartland.batterybutler.presentationfeature.home.HomeScreenList
import com.chriscartland.batterybutler.presentationmodel.home.HomeUiState
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(showBackground = true)
@Composable
fun HomeScreenFilterRowPreview() {
    HomeScreenFilterRow(
        state = HomeUiState(
            groupedDevices = emptyMap(),
            deviceTypes = emptyMap(),
        ),
        onGroupOptionToggle = {},
        onGroupOptionSelected = {},
        onSortOptionToggle = {},
        onSortOptionSelected = {},
    )
}

@OptIn(ExperimentalTime::class)
@PreviewTest
@Preview(showBackground = true)
@Composable
fun HomeScreenListPreview() {
    HomeScreenList(
        state = HomeUiState(
            groupedDevices = mapOf("All" to listOf(fakeDevice)),
            deviceTypes = mapOf(fakeDeviceType.id to fakeDeviceType),
        ),
        onDeviceClick = {},
        contentPadding = PaddingValues(16.dp),
    )
}
