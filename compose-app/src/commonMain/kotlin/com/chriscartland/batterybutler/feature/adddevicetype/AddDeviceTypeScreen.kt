package com.chriscartland.batterybutler.feature.adddevicetype

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chriscartland.batterybutler.presenter.feature.adddevicetype.AddDeviceTypeContent
import com.chriscartland.batterybutler.viewmodel.adddevicetype.AddDeviceTypeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceTypeScreen(
    viewModel: AddDeviceTypeViewModel,
    onDeviceTypeAdded: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val suggestedIcon by viewModel.suggestedIcon.collectAsStateWithLifecycle()

    AddDeviceTypeContent(
        aiMessages = aiMessages,
        suggestedIcon = suggestedIcon,
        onSuggestIcon = viewModel::suggestIcon,
        onConsumeSuggestedIcon = viewModel::consumeSuggestedIcon,
        onDeviceTypeAdded = { input ->
            viewModel.addDeviceType(input)
            onDeviceTypeAdded()
        },
        onBatchAdd = viewModel::batchAddDeviceTypes,
        onBack = onBack,
        modifier = modifier,
    )
}
