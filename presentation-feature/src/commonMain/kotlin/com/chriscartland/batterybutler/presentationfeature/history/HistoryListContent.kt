package com.chriscartland.batterybutler.presentationfeature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.presentationcore.components.HistoryListItem
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationmodel.history.HistoryItemUiModel
import com.chriscartland.batterybutler.presentationmodel.history.HistoryListUiState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Composable
fun HistoryListContent(
    state: HistoryListUiState,
    onEventClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    nowInstant: Instant = Clock.System.now(),
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            HistoryListUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is HistoryListUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                ) {
                    items(state.items, key = { it.event.id }) { item ->
                        HistoryListItem(
                            event = item.event,
                            deviceName = item.deviceName,
                            deviceTypeName = item.deviceTypeName,
                            deviceLocation = item.deviceLocation,
                            modifier = Modifier.clickable {
                                onEventClick(item.event.id, item.event.deviceId)
                            },
                            nowInstant = nowInstant,
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
        // Use fixed dates for stable screenshots
        val nowInstant = Instant.parse("2026-01-18T17:00:00Z")
        val eventInstant = Instant.parse("2026-01-11T17:00:00Z") // 7 days ago
        val event = BatteryEvent("evt1", "dev1", eventInstant)
        val item = HistoryItemUiModel(event, "Kitchen Smoke", "Smoke Alarm", "Kitchen")
        val state = HistoryListUiState.Success(
            items = listOf(item),
        )
        HistoryListContent(
            state = state,
            onEventClick = { _, _ -> },
            nowInstant = nowInstant,
        )
    }
}
