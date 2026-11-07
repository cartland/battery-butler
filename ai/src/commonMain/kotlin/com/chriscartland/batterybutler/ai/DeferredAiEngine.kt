package com.chriscartland.batterybutler.ai

import com.chriscartland.batterybutler.domain.model.ai.AiEngine
import com.chriscartland.batterybutler.domain.model.ai.AiMessage
import com.chriscartland.batterybutler.domain.model.ai.ToolHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * An [AiEngine] resolved on first use rather than at construction.
 *
 * The platform builds its AI engine *before* the DI graph (the graph takes the engine as an
 * input), but the cloud engine is itself built *by* the graph — it needs the Labs backend
 * choice and sign-in. This breaks the cycle: [provider] is read lazily, once, and the flows are
 * cold, so nothing touches the graph until the engine is actually used.
 */
class DeferredAiEngine(
    provider: () -> AiEngine,
) : AiEngine {
    private val engine by lazy(provider)

    override val isAvailable: Flow<Boolean> = flow { emitAll(engine.isAvailable) }
    override val compatibility: Flow<Boolean> = flow { emitAll(engine.compatibility) }

    override suspend fun generateResponse(
        prompt: String,
        toolHandler: ToolHandler?,
    ): Flow<AiMessage> = engine.generateResponse(prompt, toolHandler)

    override suspend fun generateResponse(
        prompt: String,
        toolHandler: ToolHandler?,
        situation: suspend () -> String,
    ): Flow<AiMessage> = engine.generateResponse(prompt, toolHandler, situation)

    override suspend fun resetConversation() = engine.resetConversation()
}
