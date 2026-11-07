package com.chriscartland.batterybutler.domain.model.ai

/**
 * One entry in the chat transcript.
 *
 * [actions] is what the app did on the assistant's behalf while producing this message, as the
 * app recorded it, never as the model described it. A message can carry actions and no text
 * at all: that is how a confirmed deletion or an undo shows up in the transcript.
 */
data class AiMessage(
    val id: String,
    val role: AiRole,
    val text: String,
    val isPartial: Boolean = false,
    val hints: Map<String, String> = emptyMap(),
    val isError: Boolean = false,
    val actions: List<AiAction> = emptyList(),
)

enum class AiRole {
    USER,
    MODEL,
    SYSTEM,
    TOOL,
}
