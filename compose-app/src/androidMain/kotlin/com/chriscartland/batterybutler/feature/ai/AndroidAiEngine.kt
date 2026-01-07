package com.chriscartland.batterybutler.feature.ai

import android.content.Context
import com.chriscartland.batterybutler.domain.ai.AiEngine
import com.chriscartland.batterybutler.domain.ai.AiMessage
import com.chriscartland.batterybutler.domain.ai.AiRole
import com.chriscartland.batterybutler.domain.ai.ToolHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import java.io.File

class AndroidAiEngine(
    private val context: Context,
) : AiEngine {
    // Simple availability check based on model file existence for now
    // In real implementation, check GPU support etc.
    // Assuming model is at /data/local/tmp/llm.bin (standard for MediaPipe demos)
    // Or users push it there.
    private val modelPath = "/data/local/tmp/llm.bin" // User didn't specify, standard manual path.

    private val _isAvailable = MutableStateFlow(false)
    override val isAvailable: Flow<Boolean> = _isAvailable.asStateFlow()
    override val compatibility: Flow<Boolean> = flow { emit(true) } // Assume compatible since Android

    init {
        // Check availability
        _isAvailable.value = File(modelPath).exists()
    }

    override suspend fun generateResponse(
        prompt: String,
        toolHandler: ToolHandler?,
    ): Flow<AiMessage> =
        flow {
            // Placeholder for MediaPipe integration
            // Will implement later in this session
            emit(
                AiMessage(
                    id = "mock_response",
                    role = AiRole.MODEL,
                    text = "AI is not fully implemented yet. Please wait for the next step.",
                    isPartial = false,
                ),
            )
        }
}
