package com.chriscartland.batterybutler.feature.adddevicetype

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.chriscartland.batterybutler.ui.feature.adddevicetype.AddDeviceTypeContent

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceTypeScreen(
    viewModel: AddDeviceTypeViewModel,
    onDeviceTypeAdded: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()

    AddDeviceTypeContent(
        aiMessages = aiMessages,
        onDeviceTypeAdded = { input ->
            viewModel.addDeviceType(input)
            onDeviceTypeAdded()
        },
        onBatchAdd = viewModel::batchAddDeviceTypes,
        onBack = onBack,
        modifier = modifier,
    )
}
