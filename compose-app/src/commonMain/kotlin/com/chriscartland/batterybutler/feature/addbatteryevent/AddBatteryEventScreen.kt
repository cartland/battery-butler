package com.chriscartland.batterybutler.feature.addbatteryevent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import com.chriscartland.batterybutler.ui.feature.addbatteryevent.AddBatteryEventContent
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
