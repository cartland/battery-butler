package com.chriscartland.batterybutler.presentationfeature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DensityLarge
import androidx.compose.material.icons.filled.DensitySmall
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.action_try_again
import com.chriscartland.batterybutler.composeresources.generated.resources.add_item_card_battery_event
import com.chriscartland.batterybutler.composeresources.generated.resources.content_desc_switch_to_compact
import com.chriscartland.batterybutler.composeresources.generated.resources.content_desc_switch_to_expanded
import com.chriscartland.batterybutler.composeresources.generated.resources.empty_history_message
import com.chriscartland.batterybutler.composeresources.generated.resources.empty_history_title
import com.chriscartland.batterybutler.composeresources.generated.resources.error_load_history
import com.chriscartland.batterybutler.composeresources.generated.resources.status_loading_history
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.presentationcore.components.AddItemCard
import com.chriscartland.batterybutler.presentationcore.components.EmptyStateContent
import com.chriscartland.batterybutler.presentationcore.components.HistoryListItem
import com.chriscartland.batterybutler.presentationcore.components.IconControl
import com.chriscartland.batterybutler.presentationcore.components.LoadingWithLabel
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.Padding
import com.chriscartland.batterybutler.presentationmodel.history.HistoryItemModel
import com.chriscartland.batterybutler.presentationmodel.history.HistoryListScreenState
import com.chriscartland.batterybutler.presentationmodel.home.DensityOption
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryListContent(
    state: HistoryListScreenState,
    onEventClick: (String, String) -> Unit,
    onAddEventClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    nowInstant: Instant = Clock.System.now(),
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onDensityOptionSelected: (DensityOption) -> Unit = {},
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (state) {
                HistoryListScreenState.Loading -> {
                    LoadingWithLabel(
                        label = composeStringResource(Res.string.status_loading_history),
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                is HistoryListScreenState.Error -> {
                    EmptyStateContent(
                        icon = Icons.Default.Warning,
                        title = composeStringResource(Res.string.error_load_history),
                        message = state.message,
                        modifier = Modifier.padding(contentPadding),
                        action = {
                            Button(onClick = onRetry) {
                                Text(composeStringResource(Res.string.action_try_again))
                            }
                        },
                    )
                }

                is HistoryListScreenState.Success -> {
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
                                // History has no sort/group controls, so this row holds the density
                                // toggle alone. It is still a FlowRow for consistency with the other
                                // two filter rows -- if a control is ever added here it must be able
                                // to wrap rather than clip off the right edge.
                                val isCompact = state.densityOption == DensityOption.COMPACT
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Padding.small),
                                    verticalArrangement = Arrangement.spacedBy(Padding.small),
                                ) {
                                    IconControl(
                                        icon = if (isCompact) Icons.Default.DensitySmall else Icons.Default.DensityLarge,
                                        contentDescription = composeStringResource(
                                            if (isCompact) {
                                                Res.string.content_desc_switch_to_expanded
                                            } else {
                                                Res.string.content_desc_switch_to_compact
                                            },
                                        ),
                                        onClicked = {
                                            onDensityOptionSelected(
                                                if (isCompact) DensityOption.EXPANDED else DensityOption.COMPACT,
                                            )
                                        },
                                    )
                                }
                            }
                            item {
                                AddItemCard(composeStringResource(Res.string.add_item_card_battery_event), onAddEventClick)
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
                                    density = state.densityOption,
                                )
                            }
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
            state = HistoryListScreenState.Success(items = emptyList()),
            onEventClick = { _, _ -> },
            onAddEventClick = {},
            onRetry = {},
            nowInstant = Instant.parse("2026-01-18T17:00:00Z"),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryListContentLoadingPreview() {
    BatteryButlerTheme {
        HistoryListContent(
            state = HistoryListScreenState.Loading,
            onEventClick = { _, _ -> },
            onAddEventClick = {},
            onRetry = {},
            nowInstant = Instant.parse("2026-01-18T17:00:00Z"),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryListContentErrorPreview() {
    BatteryButlerTheme {
        HistoryListContent(
            state = HistoryListScreenState.Error("Failed to load history"),
            onEventClick = { _, _ -> },
            onAddEventClick = {},
            onRetry = {},
            nowInstant = Instant.parse("2026-01-18T17:00:00Z"),
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
        val item = HistoryItemModel(event, "Kitchen Smoke", "Smoke Alarm", "Kitchen")
        val state = HistoryListScreenState.Success(
            items = listOf(item),
        )
        HistoryListContent(
            state = state,
            onEventClick = { _, _ -> },
            onAddEventClick = {},
            onRetry = {},
            nowInstant = nowInstant,
        )
    }
}
