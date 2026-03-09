package com.chriscartland.batterybutler.presentationfeature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.empty_history_message
import com.chriscartland.batterybutler.composeresources.generated.resources.empty_history_title
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.presentationcore.components.AddItemCard
import com.chriscartland.batterybutler.presentationcore.components.EmptyStateContent
import com.chriscartland.batterybutler.presentationcore.components.HistoryListItem
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.Padding
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
    onAddEventClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    nowInstant: Instant = Clock.System.now(),
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            HistoryListUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is HistoryListUiState.Error -> {
                EmptyStateContent(
                    icon = Icons.Default.Warning,
                    title = "Something went wrong",
                    message = state.message,
                    modifier = Modifier.padding(contentPadding),
                )
            }
            is HistoryListUiState.Success -> {
                if (state.items.isEmpty()) {
                    EmptyStateContent(
                        icon = Icons.Default.History,
                        title = composeStringResource(Res.string.empty_history_title),
                        message = composeStringResource(Res.string.empty_history_message),
                        modifier = Modifier.padding(contentPadding),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = contentPadding,
                        verticalArrangement = Arrangement.spacedBy(Padding.medium),
                    ) {
                        item {
                            AddItemCard("Add a battery event", onAddEventClick)
                        }
                        items(state.items, key = { it.event.id }) { item ->
                            HistoryListItem(
                                event = item.event,
                                deviceName = item.deviceName,
                                deviceTypeName = item.deviceTypeName,
                                deviceLocation = item.deviceLocation,
                                onClick = {
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
}

@OptIn(ExperimentalTime::class)
@Preview(showBackground = true)
@Composable
fun HistoryListContentEmptyPreview() {
    BatteryButlerTheme {
        HistoryListContent(
            state = HistoryListUiState.Success(items = emptyList()),
            onEventClick = { _, _ -> },
            onAddEventClick = {},
            nowInstant = Instant.parse("2026-01-18T17:00:00Z"),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryListContentLoadingPreview() {
    BatteryButlerTheme {
        HistoryListContent(
            state = HistoryListUiState.Loading,
            onEventClick = { _, _ -> },
            onAddEventClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryListContentErrorPreview() {
    BatteryButlerTheme {
        HistoryListContent(
            state = HistoryListUiState.Error("Failed to load history"),
            onEventClick = { _, _ -> },
            onAddEventClick = {},
        )
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
            onAddEventClick = {},
            nowInstant = nowInstant,
        )
    }
}
