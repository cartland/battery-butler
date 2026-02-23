package com.chriscartland.batterybutler.domain.model.ai

data class AiMessage(
    val id: String,
    val role: AiRole,
    val text: String,
    val isPartial: Boolean = false,
    val hints: Map<String, String> = emptyMap(),
)

enum class AiRole {
    USER,
    MODEL,
    SYSTEM,
    TOOL,
}
