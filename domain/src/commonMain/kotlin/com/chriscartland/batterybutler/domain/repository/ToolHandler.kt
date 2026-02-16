package com.chriscartland.batterybutler.domain.repository

fun interface ToolHandler {
    suspend fun execute(
        name: String,
        args: Map<String, Any?>,
    ): String
}
