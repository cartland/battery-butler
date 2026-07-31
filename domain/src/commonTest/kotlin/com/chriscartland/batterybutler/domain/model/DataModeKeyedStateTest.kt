package com.chriscartland.batterybutler.domain.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

private fun keyFor(mode: DataMode): String =
    when (mode) {
        is DataMode.LabsProd -> "prod"
        else -> "staging"
    }

class DataModeKeyedStateTest {
    @Test
    fun `switching key reads back the default - not the previous key's value`() =
        runBlocking {
            val mode = MutableStateFlow<DataMode>(DataMode.LabsProd(null))
            val state = DataModeKeyedState(mode, ::keyFor, default = "unauthenticated")

            state.setCurrent("authenticated-as-prod-user")
            assertEquals("authenticated-as-prod-user", state.current.first())

            // This is the exact bug this class exists to prevent: switching the data mode
            // must never leak the previous mode's value into the newly-selected one.
            mode.value = DataMode.LabsStaging(null)
            assertEquals("unauthenticated", state.current.first())
        }

    @Test
    fun `switching back to a previously-set key restores that key's own value`() =
        runBlocking {
            val mode = MutableStateFlow<DataMode>(DataMode.LabsProd(null))
            val state = DataModeKeyedState(mode, ::keyFor, default = "unauthenticated")

            state.setCurrent("prod-value")
            mode.value = DataMode.LabsStaging(null)
            state.setCurrent("staging-value")
            mode.value = DataMode.LabsProd(null)

            assertEquals("prod-value", state.current.first())
        }

    @Test
    fun `updateCurrent reads and writes only the currently-selected key`() =
        runBlocking {
            val mode = MutableStateFlow<DataMode>(DataMode.LabsProd(null))
            val state = DataModeKeyedState(mode, ::keyFor, default = 0)

            state.updateCurrent { it + 1 }
            state.updateCurrent { it + 1 }
            assertEquals(2, state.current.first())

            mode.value = DataMode.LabsStaging(null)
            assertEquals(0, state.current.first())
        }
}
