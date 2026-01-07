package com.chriscartland.blanket.feature.history

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
import com.chriscartland.blanket.ui.components.HistoryListItem

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
                    items(state.events) { event ->
                        HistoryListItem(
                            event = event,
                            modifier = Modifier.clickable {
                                onEventClick(event.id, event.deviceId)
                            },
                        )
                    }
                }
            }
        }
    }
}
