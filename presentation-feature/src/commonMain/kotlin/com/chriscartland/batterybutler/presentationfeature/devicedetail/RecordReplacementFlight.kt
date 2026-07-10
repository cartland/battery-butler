package com.chriscartland.batterybutler.presentationfeature.devicedetail

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import kotlin.math.roundToInt

/**
 * Timing and shape constants for the record-replacement flight animation.
 *
 * The corner radii mirror the theme tokens of the two endpoints: the record
 * button uses `MaterialTheme.shapes.large` (16.dp) and `ButlerListItemCard`
 * uses `MaterialTheme.shapes.medium` (12.dp).
 */
internal object RecordFlightSpec {
    const val FLIGHT_DURATION_MS = 550

    /** Progress at which the ghost starts handing off to the real list item. */
    const val REVEAL_START = 0.7f

    /** Progress by which the ghost has fully faded in after detaching from the button. */
    const val GHOST_FADE_IN_END = 0.15f

    /** Give up waiting for the tapped event to appear in state after this long. */
    const val AWAIT_NEW_EVENT_TIMEOUT_MS = 2_000L

    /** One layout pass is enough for an on-screen landing spot to report bounds. */
    const val TARGET_QUICK_WAIT_MS = 150L

    /** Longer budget for the landing spot to report bounds after a scroll. */
    const val TARGET_SCROLL_WAIT_MS = 500L

    /** Scale the landing item grows from as it crossfades in under the ghost. */
    const val ITEM_REVEAL_START_SCALE = 0.96f

    /** Tactile press feedback on the record button. */
    const val BUTTON_PRESSED_SCALE = 0.97f

    val SourceCornerRadius = 16.dp
    val TargetCornerRadius = 12.dp
    val SourceElevation = 8.dp
    val TargetElevation = 1.dp
}

internal object RecordFlightTestTags {
    const val GHOST = "record_flight_ghost"
    const val RECORD_BUTTON = "record_replacement_button"
}

/**
 * Drives the "record replacement" flight: a ghost card that detaches from the
 * tapped button and flies down into the spot where the newly created event
 * lands in the history list.
 *
 * The UI never learns the new event's id directly (the ViewModel generates it),
 * so the state machine arms on button tap ([onRecordPressed]) and fires when an
 * id it has never seen before shows up in the events list ([onEventsUpdated]).
 * Events that arrive without a preceding tap (e.g. background sync) never
 * trigger a flight.
 */
@Stable
internal class RecordFlightState {
    /** Bounds of the record button, in root coordinates. */
    var sourceBounds: Rect? by mutableStateOf(null)

    /** Live bounds of the in-flight event's list item, in root coordinates. */
    var targetBounds: Rect? by mutableStateOf(null)

    /** Root-coordinates origin of the overlay container, for root → local conversion. */
    var overlayOrigin: Offset by mutableStateOf(Offset.Zero)

    /** Armed by a button tap; consumed when the new event id appears in state. */
    var awaitingNewEvent: Boolean by mutableStateOf(false)
        private set

    /** The event currently in flight; its real list item stays hidden until landing. */
    var flightEventId: String? by mutableStateOf(null)
        private set

    private var knownIds: Set<String> by mutableStateOf(emptySet())

    /** Flight progress from source (0f) to target (1f). */
    val progress = Animatable(0f)

    val isInFlight: Boolean get() = flightEventId != null

    fun onRecordPressed() {
        awaitingNewEvent = true
    }

    fun cancelAwait() {
        awaitingNewEvent = false
    }

    fun onEventsUpdated(ids: List<String>) {
        if (awaitingNewEvent) {
            val newId = ids.firstOrNull { it !in knownIds }
            if (newId != null) {
                awaitingNewEvent = false
                flightEventId = newId
            }
        }
        knownIds = ids.toSet()
    }

    /** Ends the flight for [id], unless a newer flight has already replaced it. */
    fun endFlight(id: String) {
        if (flightEventId == id) {
            flightEventId = null
            targetBounds = null
        }
    }

    /**
     * Alpha for the history item with [id]. The item that is about to be (or is
     * being) animated stays invisible until the ghost is close to landing, then
     * crossfades in as the ghost fades out.
     */
    fun itemAlpha(id: String): Float =
        when {
            flightEventId == id -> revealFraction
            awaitingNewEvent && id !in knownIds -> 0f
            else -> 1f
        }

    private val revealFraction: Float
        get() = (
            (progress.value - RecordFlightSpec.REVEAL_START) /
                (1f - RecordFlightSpec.REVEAL_START)
        ).coerceIn(0f, 1f)
}

@Composable
internal fun rememberRecordFlightState(): RecordFlightState = remember { RecordFlightState() }

/**
 * The ghost card that flies from the record button to the new history item.
 * Rendered in an overlay Box above the list; it tracks [RecordFlightState.targetBounds]
 * live, so it homes onto the landing spot even while the list is scrolling.
 */
@Composable
internal fun RecordFlightGhost(
    flight: RecordFlightState,
    modifier: Modifier = Modifier,
) {
    if (!flight.isInFlight) return
    val source = flight.sourceBounds ?: return
    val progress = flight.progress.value
    val target = flight.targetBounds ?: source
    val rect = lerp(source, target, progress)
        .translate(-flight.overlayOrigin.x, -flight.overlayOrigin.y)
    val cornerRadius = lerp(RecordFlightSpec.SourceCornerRadius, RecordFlightSpec.TargetCornerRadius, progress)
    val elevation = lerp(RecordFlightSpec.SourceElevation, RecordFlightSpec.TargetElevation, progress)
    val containerColor = lerp(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.surfaceContainerHighest,
        progress,
    )
    val iconTint = lerp(
        MaterialTheme.colorScheme.onPrimary,
        MaterialTheme.colorScheme.onSurfaceVariant,
        progress,
    )
    val ghostAlpha = ghostAlpha(progress)
    val density = LocalDensity.current
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .testTag(RecordFlightTestTags.GHOST)
            .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
            .size(
                width = with(density) { rect.width.toDp() },
                height = with(density) { rect.height.toDp() },
            ).graphicsLayer { alpha = ghostAlpha }
            .shadow(elevation, shape)
            .background(containerColor, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.BatteryFull,
            contentDescription = null,
            tint = iconTint,
        )
    }
}

/** Fade in as the ghost detaches from the button; fade out as the real item takes over. */
private fun ghostAlpha(progress: Float): Float =
    when {
        progress < RecordFlightSpec.GHOST_FADE_IN_END -> {
            progress / RecordFlightSpec.GHOST_FADE_IN_END
        }

        progress > RecordFlightSpec.REVEAL_START -> {
            1f - (progress - RecordFlightSpec.REVEAL_START) / (1f - RecordFlightSpec.REVEAL_START)
        }

        else -> {
            1f
        }
    }

/**
 * Scrolls just far enough that the item with [key] is fully visible above the
 * bottom edge of the viewport. No-op if the item is not composed or already
 * fully visible.
 */
internal suspend fun scrollItemFullyIntoView(
    listState: LazyListState,
    key: Any,
) {
    val info = listState.layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return
    val viewportBottom = info.viewportEndOffset - info.afterContentPadding
    val overflowPx = (item.offset + item.size) - viewportBottom
    if (overflowPx > 0) {
        listState.animateScrollBy(overflowPx.toFloat())
    }
}
