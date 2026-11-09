package com.chriscartland.batterybutler.domain.model.ai

/**
 * One entry in the chat transcript.
 *
 * [cardId] names the [AiCard] of changes this reply proposed, and [lookups] says what the assistant
 * read to answer it, both recorded by the app rather than described by the model.
 */
data class AiMessage(
    val id: String,
    val role: AiRole,
    val text: String,
    val isPartial: Boolean = false,
    val hints: Map<String, String> = emptyMap(),
    val isError: Boolean = false,
    val cardId: String? = null,
    val lookups: List<String> = emptyList(),
)

enum class AiRole {
    USER,
    MODEL,
    SYSTEM,
    TOOL,
}
