package com.chriscartland.batterybutler.feature.adddevicetype

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.chriscartland.batterybutler.ui.feature.adddevicetype.AddDeviceTypeContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceTypeScreen(
    viewModel: AddDeviceTypeViewModel,
    onDeviceTypeAdded: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AddDeviceTypeContent(
        onDeviceTypeAdded = { input ->
            viewModel.addDeviceType(input)
            onDeviceTypeAdded()
        },
        onBack = onBack,
        modifier = modifier,
    )
}
