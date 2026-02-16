package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.ai.AiEngine
import com.chriscartland.batterybutler.domain.model.ai.AiMessage
import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject

/**
 * Sends a user message to the AI engine with device tool handling.
 *
 * Returns a [Flow] of [AiMessage] representing the AI's response,
 * which may include tool executions (adding devices, recording events, etc.).
 */
@Inject
class SendChatMessageUseCase(
    private val aiEngine: AiEngine,
    private val toolHandler: DeviceToolHandler,
) {
    suspend operator fun invoke(message: String): Flow<AiMessage> = aiEngine.generateResponse(message, toolHandler)
}
