package com.chriscartland.batterybutler.composeapp.feature.adddevicetype

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chriscartland.batterybutler.presentationfeature.adddevicetype.AddDeviceTypeContent
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
    val usedIcons by viewModel.usedIcons.collectAsStateWithLifecycle()

    AddDeviceTypeContent(
        aiMessages = aiMessages,
        suggestedIcon = suggestedIcon,
        usedIcons = usedIcons,
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
