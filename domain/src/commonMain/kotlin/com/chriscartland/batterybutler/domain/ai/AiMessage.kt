package com.chriscartland.batterybutler.domain.ai

data class AiMessage(
    val id: String,
    val role: AiRole,
    val text: String,
    val isPartial: Boolean = false,
)

enum class AiRole {
    USER, MODEL, SYSTEM, TOOL
}
