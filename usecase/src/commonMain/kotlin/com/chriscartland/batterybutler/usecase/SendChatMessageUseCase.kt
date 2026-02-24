package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.ai.AiEngine
import com.chriscartland.batterybutler.domain.model.ai.AiMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.tatarka.inject.annotations.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Sends a user message to the AI engine with device tool handling.
 *
 * Returns a [Flow] of [AiMessage] representing the AI's response,
 * which may include tool executions (adding devices, recording events, etc.).
 */
@Inject
class SendChatMessageUseCase(
    private val aiEngine: AiEngine,
    private val toolHandler: DeviceToolHandler,
    private val buildAiContextUseCase: BuildAiContextUseCase,
) {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(message: String): Flow<AiMessage> {
        val timeContext = buildTimeContext()
        val userContext = buildAiContextUseCase()
        val augmentedMessage = "$timeContext\n\n$userContext\n\n$message"
        return aiEngine.generateResponse(augmentedMessage, toolHandler)
    }

    @OptIn(ExperimentalTime::class)
    private fun buildTimeContext(): String {
        val tz = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val localDateTime = now.toLocalDateTime(tz)
        return "[Context: Current date/time: $localDateTime ($tz)]"
    }
}
