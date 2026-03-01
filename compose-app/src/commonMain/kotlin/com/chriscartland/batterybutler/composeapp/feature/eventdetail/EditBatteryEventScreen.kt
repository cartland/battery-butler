package com.chriscartland.batterybutler.composeapp.feature.eventdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chriscartland.batterybutler.presentationfeature.eventdetail.EditBatteryEventContent
import com.chriscartland.batterybutler.viewmodel.eventdetail.EditBatteryEventViewModel

@Composable
fun EditBatteryEventScreen(
    viewModel: EditBatteryEventViewModel,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EditBatteryEventContent(
        uiState = uiState,
        onSave = { date, batteryType, notes ->
            viewModel.updateEvent(date, batteryType, notes)
            onBack()
        },
        onDelete = {
            viewModel.deleteEvent()
            onDelete()
        },
        onBack = onBack,
        modifier = modifier,
    )
}
