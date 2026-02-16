package com.chriscartland.batterybutler.domain.repository

import com.chriscartland.batterybutler.domain.model.AiMessage
import kotlinx.coroutines.flow.Flow

interface AiEngine {
    val isAvailable: Flow<Boolean>
    val compatibility: Flow<Boolean>

    suspend fun generateResponse(
        prompt: String,
        toolHandler: ToolHandler? = null,
    ): Flow<AiMessage>
}
