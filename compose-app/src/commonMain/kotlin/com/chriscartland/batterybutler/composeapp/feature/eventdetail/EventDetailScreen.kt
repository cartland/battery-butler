package com.chriscartland.batterybutler.composeapp.feature.eventdetail

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chriscartland.batterybutler.presentationfeature.eventdetail.EventDetailContent
import com.chriscartland.batterybutler.viewmodel.eventdetail.EventDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    viewModel: EventDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
