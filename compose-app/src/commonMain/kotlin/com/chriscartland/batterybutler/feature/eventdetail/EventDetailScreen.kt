package com.chriscartland.batterybutler.feature.eventdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.ExperimentalMaterial3Api
import com.chriscartland.batterybutler.ui.feature.eventdetail.EventDetailContent
import com.chriscartland.batterybutler.viewmodel.eventdetail.EventDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    viewModel: EventDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    EventDetailContent(
        uiState = uiState,
        onUpdateDate = viewModel::updateDate,
        onDeleteEvent = {
            viewModel.deleteEvent()
            onBack()
        },
        onBack = onBack,
        modifier = modifier,
    )
}

