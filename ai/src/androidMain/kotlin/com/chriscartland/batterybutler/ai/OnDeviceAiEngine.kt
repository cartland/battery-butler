package com.chriscartland.batterybutler.ai

import android.content.Context
import com.chriscartland.batterybutler.domain.model.ai.AiEngine
import com.chriscartland.batterybutler.domain.model.ai.AiMessage
import com.chriscartland.batterybutler.domain.model.ai.AiRole
import com.chriscartland.batterybutler.domain.model.ai.ToolHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Placeholder for on-device AI (e.g. MediaPipe). Not yet implemented.
 */
class OnDeviceAiEngine(
    @Suppress("unused") private val context: Context,
) : AiEngine {
    override val isAvailable: Flow<Boolean> = flowOf(false)
    override val compatibility: Flow<Boolean> = flowOf(false)

    override suspend fun generateResponse(
        prompt: String,
        toolHandler: ToolHandler?,
    ): Flow<AiMessage> =
        flowOf(
            AiMessage(
                id = "on-device-unavailable",
                role = AiRole.MODEL,
                text = "On-device AI is not yet available.",
                isPartial = false,
            ),
        )
}
