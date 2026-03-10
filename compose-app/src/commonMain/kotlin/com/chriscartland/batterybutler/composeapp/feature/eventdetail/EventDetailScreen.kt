package com.chriscartland.batterybutler.composeapp.feature.eventdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chriscartland.batterybutler.presentationfeature.eventdetail.EventDetailContent
import com.chriscartland.batterybutler.viewmodel.eventdetail.EventDetailViewModel

@Composable
fun EventDetailScreen(
    viewModel: EventDetailViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDeviceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    EventDetailContent(
        uiState = uiState,
        onBack = onBack,
        onEdit = onEdit,
        onDelete = {
            viewModel.deleteEvent()
            onDelete()
        },
        onDeviceClick = onDeviceClick,
        modifier = modifier,
    )
}
