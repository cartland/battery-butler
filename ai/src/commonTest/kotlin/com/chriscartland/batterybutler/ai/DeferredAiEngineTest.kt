package com.chriscartland.batterybutler.ai

import com.chriscartland.batterybutler.domain.model.ai.AiEngine
import com.chriscartland.batterybutler.domain.model.ai.AiMessage
import com.chriscartland.batterybutler.domain.model.ai.AiRole
import com.chriscartland.batterybutler.domain.model.ai.ToolHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DeferredAiEngineTest {
    private class Fixed : AiEngine {
        override val isAvailable: Flow<Boolean> = flowOf(true)
        override val compatibility: Flow<Boolean> = flowOf(false)

        override suspend fun generateResponse(
            prompt: String,
            toolHandler: ToolHandler?,
        ): Flow<AiMessage> = flowOf(AiMessage("1", AiRole.MODEL, "echo $prompt"))
    }

    @Test
    fun `the provider is not read at construction and is read once`() =
        runTest {
            var reads = 0
            val engine = DeferredAiEngine {
                reads++
                Fixed()
            }
            // Reading the flow properties must not resolve the engine — the platform builds
            // DynamicAiEngine (which reads them) before the graph the provider reaches into.
            val available = engine.isAvailable
            engine.compatibility
            assertEquals(0, reads)

            assertEquals(true, available.first())
            assertEquals(false, engine.compatibility.first())
            assertEquals("echo hi", engine.generateResponse("hi").first().text)
            assertEquals(1, reads)
        }
}
