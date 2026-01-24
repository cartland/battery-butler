package com.chriscartland.batterybutler.ai

fun interface ToolHandler {
    suspend fun execute(
        name: String,
        args: Map<String, Any?>,
    ): String
}
