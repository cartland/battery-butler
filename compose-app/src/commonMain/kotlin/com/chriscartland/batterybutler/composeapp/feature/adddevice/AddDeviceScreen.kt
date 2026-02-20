package com.chriscartland.batterybutler.composeapp.feature.adddevice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chriscartland.batterybutler.presentationfeature.adddevice.AddDeviceContent
import com.chriscartland.batterybutler.viewmodel.adddevice.AddDeviceViewModel

@Composable
fun AddDeviceScreen(
    viewModel: AddDeviceViewModel,
    onDeviceAdded: () -> Unit,
    onAddDeviceTypeClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val deviceTypes by viewModel.deviceTypes.collectAsStateWithLifecycle()
    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val isAiBatchImportEnabled by viewModel.isAiBatchImportEnabled.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    AddDeviceContent(
        deviceTypes = deviceTypes,
        aiMessages = aiMessages,
        isAiBatchImportEnabled = isAiBatchImportEnabled,
        onAddDevice = { input ->
            viewModel.addDevice(input)
            onDeviceAdded()
        },
        onBatchAdd = viewModel::batchAddDevices,
        onAddDeviceTypeClick = onAddDeviceTypeClick,
        onBack = onBack,
        modifier = modifier,
        isLoading = isLoading,
    )
}
