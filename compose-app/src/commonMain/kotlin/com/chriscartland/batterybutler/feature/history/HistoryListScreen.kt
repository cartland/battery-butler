package com.chriscartland.batterybutler.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.chriscartland.batterybutler.ui.components.HistoryListItem
import com.chriscartland.batterybutler.viewmodel.history.HistoryListUiState
import com.chriscartland.batterybutler.viewmodel.history.HistoryListViewModel

@Composable
fun HistoryListScreen(
    viewModel: HistoryListViewModel,
    onEventClick: (String, String) -> Unit, // eventId, deviceId
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
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
