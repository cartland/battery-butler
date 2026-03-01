package com.chriscartland.batterybutler.presentationfeature.devicetypes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.action_edit
import com.chriscartland.batterybutler.composeresources.generated.resources.label_quantity
import com.chriscartland.batterybutler.composeresources.generated.resources.label_type
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.presentationcore.components.DeviceIconMapper
import com.chriscartland.batterybutler.presentationcore.components.DeviceListItem
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.Padding
import com.chriscartland.batterybutler.presentationfeature.devicedetail.StatCard
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeDetailUiState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun DeviceTypeDetailContent(
    state: DeviceTypeDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeviceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    nowInstant: Instant = Clock.System.now(),
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ButlerCenteredTopAppBar(
                title = "Device Type",
                onBack = onBack,
                actions = {
                    TextButton(onClick = onEdit) {
                        Text(
                            composeStringResource(Res.string.action_edit),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (state) {
                DeviceTypeDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                DeviceTypeDetailUiState.NotFound -> {
                    Text("Device type not found", modifier = Modifier.align(Alignment.Center))
                }
                is DeviceTypeDetailUiState.Success -> {
                    DeviceTypeDetailBody(
                        state = state,
                        onDeviceClick = onDeviceClick,
                        modifier = Modifier.fillMaxSize(),
                        nowInstant = nowInstant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun DeviceTypeDetailBody(
    state: DeviceTypeDetailUiState.Success,
    onDeviceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    nowInstant: Instant = Clock.System.now(),
) {
    val deviceType = state.deviceType
    val iconName = deviceType.defaultIcon ?: "devices_other"
    val typeLabel = composeStringResource(Res.string.label_type)
    val quantityLabel = composeStringResource(Res.string.label_quantity)

    LazyColumn(
        modifier = modifier.padding(horizontal = Padding.standard),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Profile Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.padding(bottom = 16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = DeviceIconMapper.getIcon(iconName),
                            contentDescription = "Device type icon",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Text(
                    text = deviceType.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Stats Grid
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Padding.standard)) {
                StatCard(
                    icon = Icons.Default.BatteryFull,
                    label = typeLabel,
                    value = deviceType.batteryType,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    icon = Icons.Default.Numbers,
                    label = quantityLabel,
                    value = "${deviceType.batteryQuantity}",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Devices Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Devices",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${state.devices.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(state.devices, key = { it.id }) { device ->
            DeviceListItem(
                device = device,
                deviceType = deviceType,
                onClick = { onDeviceClick(device.id) },
                nowInstant = nowInstant,
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalTime::class)
@Preview(showBackground = true)
@Composable
fun DeviceTypeDetailContentPreview() {
    BatteryButlerTheme {
        val nowInstant = Instant.parse("2026-01-18T17:00:00Z")
        val type = DeviceType("type1", "Smoke Alarm", "detector_smoke")
        val device1 = Device("dev1", "Kitchen Remote", "type1", nowInstant, nowInstant, "Kitchen")
        val device2 = Device("dev2", "Living Room Remote", "type1", nowInstant, nowInstant, "Living Room")
        val state = DeviceTypeDetailUiState.Success(
            deviceType = type,
            devices = listOf(device1, device2),
        )
        DeviceTypeDetailContent(
            state = state,
            onBack = {},
            onEdit = {},
            onDeviceClick = {},
            nowInstant = nowInstant,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceTypeDetailLoadingPreview() {
    BatteryButlerTheme {
        DeviceTypeDetailContent(
            state = DeviceTypeDetailUiState.Loading,
            onBack = {},
            onEdit = {},
            onDeviceClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceTypeDetailNotFoundPreview() {
    BatteryButlerTheme {
        DeviceTypeDetailContent(
            state = DeviceTypeDetailUiState.NotFound,
            onBack = {},
            onEdit = {},
            onDeviceClick = {},
        )
    }
}
