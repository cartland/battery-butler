package com.chriscartland.batterybutler.domain.model.ai

import kotlinx.coroutines.flow.Flow

interface AiEngine {
    val isAvailable: Flow<Boolean>
    val compatibility: Flow<Boolean>

    suspend fun generateResponse(
        prompt: String,
        toolHandler: ToolHandler? = null,
    ): Flow<AiMessage>

    /**
     * Like [generateResponse], with a description of the current state of the person's data
     * that is re-read before every model call, so a tool's effect is already visible to the
     * model's next step and nothing stale accumulates in the conversation.
     *
     * An engine that keeps a conversation puts it where it won't be remembered (the cloud engine
     * uses the system instruction). This default is for engines with no conversation to keep.
     */
    suspend fun generateResponse(
        prompt: String,
        toolHandler: ToolHandler?,
        situation: suspend () -> String,
    ): Flow<AiMessage> = generateResponse("${situation()}\n\n$prompt", toolHandler)

    /** Forget the conversation so far. The next message starts a new one. */
    suspend fun resetConversation() = Unit
}
