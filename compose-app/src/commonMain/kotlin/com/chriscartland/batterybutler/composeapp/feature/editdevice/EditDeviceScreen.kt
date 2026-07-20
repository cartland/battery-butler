package com.chriscartland.batterybutler.composeapp.feature.editdevice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chriscartland.batterybutler.presentationfeature.editdevice.EditDeviceContent
import com.chriscartland.batterybutler.viewmodel.editdevice.EditDeviceViewModel

@Composable
fun EditDeviceScreen(
    viewModel: EditDeviceViewModel,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onAddDeviceTypeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val photoError by viewModel.photoError.collectAsStateWithLifecycle()

    EditDeviceContent(
        uiState = uiState,
        onSave = { input ->
            viewModel.updateDevice(input)
            onBack()
        },
        onDelete = {
            viewModel.deleteDevice()
            onDelete()
        },
        onAddDeviceTypeClick = onAddDeviceTypeClick,
        onBack = onBack,
        modifier = modifier,
        onPhotoPicked = { bytes, contentType -> viewModel.uploadPhoto(bytes, contentType) },
        onRemovePhoto = { viewModel.removePhoto() },
        photoError = photoError,
    )
}
