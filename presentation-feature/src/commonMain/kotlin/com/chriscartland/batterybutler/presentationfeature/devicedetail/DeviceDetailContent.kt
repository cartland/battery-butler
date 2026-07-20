package com.chriscartland.batterybutler.presentationfeature.devicedetail

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.action_edit
import com.chriscartland.batterybutler.composeresources.generated.resources.action_record_replacement
import com.chriscartland.batterybutler.composeresources.generated.resources.action_record_replacement_description
import com.chriscartland.batterybutler.composeresources.generated.resources.action_view_all
import com.chriscartland.batterybutler.composeresources.generated.resources.content_desc_device_icon
import com.chriscartland.batterybutler.composeresources.generated.resources.content_desc_device_type
import com.chriscartland.batterybutler.composeresources.generated.resources.content_desc_location
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
import com.chriscartland.batterybutler.presentationcore.components.rememberDeviceImageBitmap
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.Padding
import com.chriscartland.batterybutler.presentationmodel.devicedetail.DeviceDetailScreenState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun DeviceDetailContent(
    state: DeviceDetailScreenState,
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
                DeviceDetailScreenState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                DeviceDetailScreenState.NotFound -> {
                    Text(composeStringResource(Res.string.error_device_not_found), modifier = Modifier.align(Alignment.Center))
                }

                is DeviceDetailScreenState.Success -> {
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
    state: DeviceDetailScreenState.Success,
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

    val listState = rememberLazyListState()
    val flight = rememberRecordFlightState()

    // Detect the tap-created event arriving in state (the ViewModel generates its id,
    // so the UI recognizes it as the first id it has never seen before).
    val eventIds = remember(state.events) { state.events.map { it.id } }
    LaunchedEffect(eventIds) { flight.onEventsUpdated(eventIds) }

    // Don't stay armed forever if the tapped event never lands (e.g. write failed).
    LaunchedEffect(flight.awaitingNewEvent) {
        if (flight.awaitingNewEvent) {
            delay(RecordFlightSpec.AWAIT_NEW_EVENT_TIMEOUT_MS)
            flight.cancelAwait()
        }
    }

    // Orchestrate a flight: make sure the landing spot is on screen, then fly the ghost.
    LaunchedEffect(flight.flightEventId) {
        val id = flight.flightEventId ?: return@LaunchedEffect
        try {
            // Give the new row one layout pass to report its landing bounds.
            var target = withTimeoutOrNull(RecordFlightSpec.TARGET_QUICK_WAIT_MS) {
                snapshotFlow { flight.targetBounds }.filterNotNull().first()
            }
            if (target == null) {
                // The row never got composed — it's off screen. Scroll down to it.
                val eventIndex = state.events.indexOfFirst { it.id == id }
                // Leading item count is derived, not hardcoded: total = leading + events + trailing spacer.
                val leadingItems = listState.layoutInfo.totalItemsCount - state.events.size - 1
                if (eventIndex >= 0 && leadingItems >= 0) {
                    listState.animateScrollToItem(leadingItems + eventIndex)
                }
                target = withTimeoutOrNull(RecordFlightSpec.TARGET_SCROLL_WAIT_MS) {
                    snapshotFlow { flight.targetBounds }.filterNotNull().first()
                }
            } else {
                // On screen but possibly cut off at the bottom — nudge while the ghost flies.
                launch { scrollItemFullyIntoView(listState, id) }
            }
            if (target != null && flight.sourceBounds != null) {
                flight.progress.snapTo(0f)
                flight.progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = RecordFlightSpec.FLIGHT_DURATION_MS,
                        easing = FastOutSlowInEasing,
                    ),
                )
            }
        } finally {
            flight.endFlight(id)
        }
    }

    Box(
        modifier = modifier.onGloballyPositioned {
            flight.overlayOrigin = it.boundsInRoot().topLeft
        },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = Padding.standard),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Profile Header
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.padding(bottom = Padding.standard)) {
                        val imageBitmap = rememberDeviceImageBitmap(state.imageBytes)
                        Box(
                            modifier = Modifier
                                .size(112.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (imageBitmap != null) {
                                Image(
                                    bitmap = imageBitmap,
                                    contentDescription = composeStringResource(Res.string.content_desc_device_icon),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Icon(
                                    imageVector = DeviceIconMapper.getIcon(iconName),
                                    contentDescription = composeStringResource(Res.string.content_desc_device_icon),
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
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
                        modifier = Modifier.padding(top = Padding.extraSmall),
                    ) {
                        Icon(
                            Icons.Default.DevicesOther,
                            contentDescription = composeStringResource(Res.string.content_desc_device_type),
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
                            modifier = Modifier.padding(top = Padding.extraSmall),
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = composeStringResource(Res.string.content_desc_location),
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Padding.standard)) {
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
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val pressScale by animateFloatAsState(
                    targetValue = if (isPressed) RecordFlightSpec.BUTTON_PRESSED_SCALE else 1f,
                    label = "recordButtonPressScale",
                )
                val haptics = LocalHapticFeedback.current
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        flight.onRecordPressed()
                        onRecordReplacement()
                    },
                    modifier = Modifier
                        .testTag(RecordFlightTestTags.RECORD_BUTTON)
                        .fillMaxWidth()
                        .height(80.dp)
                        .graphicsLayer {
                            scaleX = pressScale
                            scaleY = pressScale
                        }.onGloballyPositioned { flight.sourceBounds = it.boundsInRoot() }
                        .shadow(8.dp, MaterialTheme.shapes.large),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    interactionSource = interactionSource,
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
                                        MaterialTheme.shapes.small,
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Padding.extraSmall),
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
                    onClick = { onEventClick(event.id) },
                    nowInstant = nowInstant,
                    modifier = Modifier
                        .animateItem()
                        .graphicsLayer {
                            // The in-flight item stays hidden, then crossfades in with a
                            // slight grow as the ghost lands on top of it.
                            val revealAlpha = flight.itemAlpha(event.id)
                            alpha = revealAlpha
                            val scale = RecordFlightSpec.ITEM_REVEAL_START_SCALE +
                                (1f - RecordFlightSpec.ITEM_REVEAL_START_SCALE) * revealAlpha
                            scaleX = scale
                            scaleY = scale
                        }.onGloballyPositioned {
                            if (event.id == flight.flightEventId) {
                                flight.targetBounds = it.boundsInRoot()
                            }
                        },
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        RecordFlightGhost(flight)
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
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), MaterialTheme.shapes.large)
            .padding(Padding.standard),
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
        val state = DeviceDetailScreenState.Success(
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
            state = DeviceDetailScreenState.Loading,
            onRecordReplacement = {},
            onBack = {},
            onEdit = {},
            onEventClick = {},
            nowInstant = Instant.parse("2026-01-18T17:00:00Z"),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceDetailNotFoundPreview() {
    BatteryButlerTheme {
        DeviceDetailContent(
            state = DeviceDetailScreenState.NotFound,
            onRecordReplacement = {},
            onBack = {},
            onEdit = {},
            onEventClick = {},
            nowInstant = Instant.parse("2026-01-18T17:00:00Z"),
        )
    }
}
