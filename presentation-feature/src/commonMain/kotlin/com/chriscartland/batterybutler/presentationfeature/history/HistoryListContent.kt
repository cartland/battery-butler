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
import com.chriscartland.batterybutler.presentationcore.components.HistoryListItem
import com.chriscartland.batterybutler.presentationmodel.history.HistoryListUiState

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
