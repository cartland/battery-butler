package com.chriscartland.batterybutler.domain.model

data class AiMessage(
    val id: String,
    val role: AiRole,
    val text: String,
    val isPartial: Boolean = false,
)

enum class AiRole {
    USER,
    MODEL,
    SYSTEM,
    TOOL,
}
