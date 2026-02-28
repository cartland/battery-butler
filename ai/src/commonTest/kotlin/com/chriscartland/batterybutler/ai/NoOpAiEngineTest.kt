package com.chriscartland.batterybutler.ai

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoOpAiEngineTest {
    @Test
    fun `isAvailable emits false`() =
        runTest {
            val result = NoOpAiEngine.isAvailable.first()

            assertFalse(result)
        }

    @Test
    fun `compatibility emits false`() =
        runTest {
            val result = NoOpAiEngine.compatibility.first()

            assertFalse(result)
        }

    @Test
    fun `generateResponse returns empty flow`() =
        runTest {
            val result = NoOpAiEngine.generateResponse("test").toList()

            assertTrue(result.isEmpty())
        }
}
