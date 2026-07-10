package com.chriscartland.batterybutler.presentationfeature.devicedetail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecordFlightStateTest {
    @Test
    fun `events arriving without a tap never trigger a flight`() {
        val state = RecordFlightState()
        state.onEventsUpdated(listOf("a"))
        // A sync-arrived event shows up with no preceding button tap.
        state.onEventsUpdated(listOf("b", "a"))
        assertNull(state.flightEventId)
        assertFalse(state.awaitingNewEvent)
    }

    @Test
    fun `tap then new id starts a flight for that id`() {
        val state = RecordFlightState()
        state.onEventsUpdated(listOf("a"))
        state.onRecordPressed()
        assertTrue(state.awaitingNewEvent)
        state.onEventsUpdated(listOf("new", "a"))
        assertEquals("new", state.flightEventId)
        assertFalse(state.awaitingNewEvent)
    }

    @Test
    fun `tap followed by an unchanged list stays armed`() {
        val state = RecordFlightState()
        state.onEventsUpdated(listOf("a"))
        state.onRecordPressed()
        state.onEventsUpdated(listOf("a"))
        assertNull(state.flightEventId)
        assertTrue(state.awaitingNewEvent)
    }

    @Test
    fun `cancelAwait disarms without a flight`() {
        val state = RecordFlightState()
        state.onEventsUpdated(listOf("a"))
        state.onRecordPressed()
        state.cancelAwait()
        state.onEventsUpdated(listOf("new", "a"))
        assertNull(state.flightEventId)
    }

    @Test
    fun `endFlight clears only the current flight`() {
        val state = RecordFlightState()
        state.onEventsUpdated(listOf("a"))
        state.onRecordPressed()
        state.onEventsUpdated(listOf("first", "a"))
        assertEquals("first", state.flightEventId)

        // A stale id (e.g. a cancelled earlier flight's cleanup) must not clobber it.
        state.endFlight("stale")
        assertEquals("first", state.flightEventId)

        state.endFlight("first")
        assertNull(state.flightEventId)
        assertNull(state.targetBounds)
    }

    @Test
    fun `in-flight item is hidden until the reveal window`() {
        val state = RecordFlightState()
        state.onEventsUpdated(listOf("a"))
        state.onRecordPressed()
        state.onEventsUpdated(listOf("new", "a"))

        // progress is 0f — the real item is fully hidden, others untouched.
        assertEquals(0f, state.itemAlpha("new"))
        assertEquals(1f, state.itemAlpha("a"))
    }

    @Test
    fun `while armed the not-yet-known item is hidden from its first frame`() {
        val state = RecordFlightState()
        state.onEventsUpdated(listOf("a"))
        state.onRecordPressed()
        // The new row composes before onEventsUpdated has seen its id — it must
        // already be hidden to avoid a one-frame flash.
        assertEquals(0f, state.itemAlpha("brand-new"))
        assertEquals(1f, state.itemAlpha("a"))
    }
}
