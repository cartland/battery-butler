package com.chriscartland.batterybutler.presentationfeature.devicedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.action_edit
import com.chriscartland.batterybutler.composeresources.generated.resources.action_record_replacement
import com.chriscartland.batterybutler.composeresources.generated.resources.action_record_replacement_description
import com.chriscartland.batterybutler.composeresources.generated.resources.action_view_all
import com.chriscartland.batterybutler.composeresources.generated.resources.device_detail_title
import com.chriscartland.batterybutler.composeresources.generated.resources.error_device_not_found
import com.chriscartland.batterybutler.composeresources.generated.resources.label_quantity
import com.chriscartland.batterybutler.composeresources.generated.resources.label_type
import com.chriscartland.batterybutler.composeresources.generated.resources.section_history
import com.chriscartland.batterybutler.composeresources.generated.resources.unknown_type
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.presentationcore.components.DeviceIconMapper
import com.chriscartland.batterybutler.presentationcore.components.HistoryListItem
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationmodel.devicedetail.DeviceDetailUiState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun DeviceDetailContent(
    state: DeviceDetailUiState,
    onRecordReplacement: () -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    nowInstant: Instant = Clock.System.now(),
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ButlerCenteredTopAppBar(
                title = composeStringResource(Res.string.device_detail_title),
                onBack = onBack,
                actions = {
                    androidx.compose.material3.TextButton(onClick = onEdit) {
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
                DeviceDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                DeviceDetailUiState.NotFound -> {
                    Text(composeStringResource(Res.string.error_device_not_found), modifier = Modifier.align(Alignment.Center))
                }
                is DeviceDetailUiState.Success -> {
                    DeviceDetailBody(
                        state = state,
                        onRecordReplacement = onRecordReplacement,
                        onEventClick = onEventClick,
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
fun DeviceDetailBody(
    state: DeviceDetailUiState.Success,
    onRecordReplacement: () -> Unit,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    nowInstant: Instant = Clock.System.now(),
) {
    val device = state.device
    val deviceType = state.deviceType
    val iconName = deviceType?.defaultIcon ?: "devices_other"
    val unknownTypeName = composeStringResource(Res.string.unknown_type)
    val typeLabel = composeStringResource(Res.string.label_type)
    val quantityLabel = composeStringResource(Res.string.label_quantity)
    val historyLabel = composeStringResource(Res.string.section_history)
    val viewAllLabel = composeStringResource(Res.string.action_view_all)
    val recordReplacementLabel = composeStringResource(Res.string.action_record_replacement)
    val recordReplacementDescription = composeStringResource(Res.string.action_record_replacement_description)

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
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
                            contentDescription = "Device icon",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Icon(
                        Icons.Default.DevicesOther,
                        contentDescription = "Device type",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = deviceType?.name ?: unknownTypeName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                }

                device.location?.takeIf { it.isNotBlank() }?.let { location ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Stats Grid
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Battery Type Card
                StatCard(
                    icon = Icons.Default.BatteryFull,
                    label = typeLabel,
                    value = deviceType?.batteryType ?: "N/A",
                    modifier = Modifier.weight(1f),
                )
                // Quantity Card
                StatCard(
                    icon = Icons.Default.Numbers,
                    label = quantityLabel,
                    value = "${deviceType?.batteryQuantity ?: 0}",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Action Section
        item {
            Button(
                onClick = onRecordReplacement,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.AddCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        Column(verticalArrangement = Arrangement.Center) {
                            Text(
                                recordReplacementLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                recordReplacementDescription,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            )
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // History Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    historyLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    viewAllLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        items(state.events, key = { it.id }) { event ->
            HistoryListItem(
                event = event,
                deviceName = device.name,
                deviceTypeName = deviceType?.name ?: unknownTypeName,
                deviceLocation = device.location,
                modifier = Modifier.clickable { onEventClick(event.id) },
                nowInstant = nowInstant,
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalTime::class)
@Preview(showBackground = true)
@Composable
fun DeviceDetailContentPreview() {
    BatteryButlerTheme {
        // Use fixed dates for stable screenshots
        val nowInstant = Instant.parse("2026-01-18T17:00:00Z")
        val eventInstant = Instant.parse("2026-01-11T17:00:00Z") // 7 days ago
        val type = DeviceType("type1", "Smoke Alarm", "detector_smoke")
        val device = Device("dev1", "Kitchen Smoke", "type1", nowInstant, nowInstant, "Kitchen")
        val event = BatteryEvent("evt1", "dev1", eventInstant)
        val state = DeviceDetailUiState.Success(
            device = device,
            deviceType = type,
            events = listOf(event),
        )
        DeviceDetailContent(
            state = state,
            onRecordReplacement = {},
            onBack = {},
            onEdit = {},
            onEventClick = {},
            nowInstant = nowInstant,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceDetailLoadingPreview() {
    BatteryButlerTheme {
        DeviceDetailContent(
            state = DeviceDetailUiState.Loading,
            onRecordReplacement = {},
            onBack = {},
            onEdit = {},
            onEventClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceDetailNotFoundPreview() {
    BatteryButlerTheme {
        DeviceDetailContent(
            state = DeviceDetailUiState.NotFound,
            onRecordReplacement = {},
            onBack = {},
            onEdit = {},
            onEventClick = {},
        )
    }
}
