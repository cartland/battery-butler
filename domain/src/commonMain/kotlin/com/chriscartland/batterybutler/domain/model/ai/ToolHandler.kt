package com.chriscartland.batterybutler.domain.model.ai

fun interface ToolHandler {
    suspend fun execute(
        name: String,
        args: Map<String, Any?>,
    ): String
}
