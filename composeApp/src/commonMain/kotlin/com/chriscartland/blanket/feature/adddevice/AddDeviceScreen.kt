package com.chriscartland.blanket.feature.adddevice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chriscartland.blanket.ui.feature.adddevice.AddDeviceScreen

@Composable
fun AddDeviceScreen(
    viewModel: AddDeviceViewModel,
    onDeviceAdded: () -> Unit,
    onManageDeviceTypesClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val deviceTypes by viewModel.deviceTypes.collectAsStateWithLifecycle()

    // Seed types if empty
    LaunchedEffect(Unit) {
        viewModel.seedDeviceTypes()
    }

    AddDeviceScreen(
        deviceTypes = deviceTypes,
        onAddDevice = { input ->
            viewModel.addDevice(input)
            onDeviceAdded()
        },
        onManageDeviceTypesClick = onManageDeviceTypesClick,
        onBack = onBack,
        modifier = modifier,
    )
}
