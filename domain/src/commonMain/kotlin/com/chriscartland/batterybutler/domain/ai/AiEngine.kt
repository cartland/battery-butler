package com.chriscartland.batterybutler.domain.ai

import kotlinx.coroutines.flow.Flow

interface AiEngine {
    val isAvailable: Flow<Boolean>
    val compatibility: Flow<Boolean>
    suspend fun generateResponse(prompt: String, toolHandler: ToolHandler? = null): Flow<AiMessage>
}
