package com.chriscartland.batterybutler.feature.ai

import com.chriscartland.batterybutler.domain.ai.AiEngine
import com.chriscartland.batterybutler.domain.ai.AiMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import com.chriscartland.batterybutler.domain.ai.ToolHandler

object NoOpAiEngine : AiEngine {
    override val isAvailable: Flow<Boolean> = flowOf(false)
    override suspend fun generateResponse(prompt: String, toolHandler: ToolHandler?): Flow<AiMessage> = flowOf()
    override val compatibility: Flow<Boolean> = flowOf(false)
}
