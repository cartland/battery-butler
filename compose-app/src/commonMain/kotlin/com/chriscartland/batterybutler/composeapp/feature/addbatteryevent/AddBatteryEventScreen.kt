package com.chriscartland.batterybutler.composeapp.feature.addbatteryevent

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chriscartland.batterybutler.presentation.feature.addbatteryevent.AddBatteryEventContent
import com.chriscartland.batterybutler.viewmodel.addbatteryevent.AddBatteryEventViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBatteryEventScreen(
    viewModel: AddBatteryEventViewModel,
    onEventAdded: () -> Unit,
    onAddDeviceClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()

    AddBatteryEventContent(
        devices = devices,
        aiMessages = aiMessages,
        onAddEvent = { deviceId, date ->
            viewModel.addEvent(
                deviceId = deviceId,
                date = date,
                batteryType = null,
                notes = null,
            )
            onEventAdded()
        },
        onBatchAdd = viewModel::batchAddEvents,
        onAddDeviceClick = onAddDeviceClick,
        onBack = onBack,
        modifier = modifier,
    )
}
