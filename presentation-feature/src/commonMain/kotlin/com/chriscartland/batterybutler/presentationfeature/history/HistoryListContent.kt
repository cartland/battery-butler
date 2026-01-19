package com.chriscartland.batterybutler.presentationfeature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.presentationcore.components.HistoryListItem
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationmodel.history.HistoryItemUiModel
import com.chriscartland.batterybutler.presentationmodel.history.HistoryListUiState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun HistoryListContent(
    state: HistoryListUiState,
    onEventClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            HistoryListUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is HistoryListUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.items) { item ->
                        HistoryListItem(
                            event = item.event,
                            deviceName = item.deviceName,
                            deviceTypeName = item.deviceTypeName,
                            deviceLocation = item.deviceLocation,
                            modifier = Modifier.clickable {
                                onEventClick(item.event.id, item.event.deviceId)
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Preview
@Composable
fun HistoryListContentPreview() {
    BatteryButlerTheme {
        val now = Clock.System.now()
        val event = BatteryEvent("evt1", "dev1", now)
        val item = HistoryItemUiModel(event, "Kitchen Smoke", "Smoke Alarm", "Kitchen")
        val state = HistoryListUiState.Success(
            items = listOf(item),
        )
        HistoryListContent(
            state = state,
            onEventClick = { _, _ -> },
        )
    }
}
