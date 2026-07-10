package com.chriscartland.batterybutler.presentationfeature.devicedetail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationmodel.devicedetail.DeviceDetailScreenState
import kotlin.test.Test
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Headless end-to-end test of the record-replacement flight: click the real
 * button, feed the new event back through state (as the ViewModel/Room flow
 * would), and drive the animation on the test's virtual clock.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalTime::class)
class RecordReplacementFlightUiTest {
    private val now = Instant.parse("2026-01-18T17:00:00Z")
    private val deviceType = DeviceType("type1", "Smoke Alarm", "detector_smoke")
    private val device = Device("dev1", "Kitchen Smoke", "type1", now, now, "Kitchen")
    private val existingEvent = BatteryEvent("evt-existing", "dev1", Instant.parse("2026-01-11T17:00:00Z"))

    @Test
    fun `tapping record flies a ghost that lands and disappears`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var events by mutableStateOf(listOf(existingEvent))
            setContent {
                BatteryButlerTheme {
                    DeviceDetailBody(
                        state = DeviceDetailScreenState.Success(device, deviceType, events),
                        onRecordReplacement = {
                            events = listOf(BatteryEvent("evt-new", "dev1", now)) + events
                        },
                        onEventClick = {},
                        nowInstant = now,
                    )
                }
            }
            mainClock.advanceTimeBy(SETTLE_MS)
            onNodeWithTag(RecordFlightTestTags.GHOST).assertDoesNotExist()
            // Device name renders in the profile header and once per history row.
            onAllNodesWithText("Kitchen Smoke").assertCountEquals(2)

            onNodeWithTag(RecordFlightTestTags.RECORD_BUTTON).performClick()

            // A few frames later the new event has landed in state and the ghost is up.
            mainClock.advanceTimeBy(FRAMES_AFTER_CLICK_MS)
            onNodeWithTag(RecordFlightTestTags.GHOST).assertExists()

            // Past bounds-wait + flight duration: ghost gone, new row present.
            mainClock.advanceTimeBy(FLIGHT_COMPLETE_MS)
            onNodeWithTag(RecordFlightTestTags.GHOST).assertDoesNotExist()
            onAllNodesWithText("Kitchen Smoke").assertCountEquals(3)
        }

    @Test
    fun `events arriving without a tap never show a ghost`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var events by mutableStateOf(listOf(existingEvent))
            setContent {
                BatteryButlerTheme {
                    DeviceDetailBody(
                        state = DeviceDetailScreenState.Success(device, deviceType, events),
                        onRecordReplacement = {},
                        onEventClick = {},
                        nowInstant = now,
                    )
                }
            }
            mainClock.advanceTimeBy(SETTLE_MS)

            // Simulate a background-sync insert: state changes with no button tap.
            events = listOf(BatteryEvent("evt-sync", "dev1", now)) + events

            mainClock.advanceTimeBy(FRAMES_AFTER_CLICK_MS)
            onNodeWithTag(RecordFlightTestTags.GHOST).assertDoesNotExist()
            mainClock.advanceTimeBy(FLIGHT_COMPLETE_MS)
            onNodeWithTag(RecordFlightTestTags.GHOST).assertDoesNotExist()
        }

    private companion object {
        /** Let initial composition, layout, and effects settle. */
        const val SETTLE_MS = 100L

        /** Enough frames for state → recomposition → flight start → ghost. */
        const val FRAMES_AFTER_CLICK_MS = 100L

        /** Comfortably past TARGET_QUICK_WAIT_MS + FLIGHT_DURATION_MS. */
        const val FLIGHT_COMPLETE_MS = 2_000L
    }
}
