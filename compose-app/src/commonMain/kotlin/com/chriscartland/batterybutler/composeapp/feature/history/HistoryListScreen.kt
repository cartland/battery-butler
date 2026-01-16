package com.chriscartland.batterybutler.composeapp.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.chriscartland.batterybutler.presentation.feature.history.HistoryListContent
import com.chriscartland.batterybutler.viewmodel.history.HistoryListViewModel

@Composable
fun HistoryListScreen(
    viewModel: HistoryListViewModel,
    onEventClick: (String, String) -> Unit, // eventId, deviceId
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    HistoryListContent(
        state = uiState,
        onEventClick = onEventClick,
        modifier = modifier,
    )
}
