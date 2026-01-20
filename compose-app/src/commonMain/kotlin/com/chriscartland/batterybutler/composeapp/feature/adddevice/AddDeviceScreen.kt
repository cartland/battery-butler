package com.chriscartland.batterybutler.composeapp.feature.adddevice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chriscartland.batterybutler.presentationfeature.adddevice.AddDeviceContent
import com.chriscartland.batterybutler.viewmodel.adddevice.AddDeviceViewModel

@Composable
fun AddDeviceScreen(
    viewModel: AddDeviceViewModel,
    onDeviceAdded: () -> Unit,
    onManageDeviceTypesClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val deviceTypes by viewModel.deviceTypes.collectAsStateWithLifecycle()

    // Seed types block removed


    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()

    AddDeviceContent(
        deviceTypes = deviceTypes,
        aiMessages = aiMessages,
        onAddDevice = { input ->
            viewModel.addDevice(input)
            onDeviceAdded()
        },
        onBatchAdd = viewModel::batchAddDevices,
        onManageDeviceTypesClick = onManageDeviceTypesClick,
        onBack = onBack,
        modifier = modifier,
    )
}
