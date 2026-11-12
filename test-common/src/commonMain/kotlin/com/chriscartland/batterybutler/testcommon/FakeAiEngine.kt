package com.chriscartland.batterybutler.testcommon

import com.chriscartland.batterybutler.domain.model.ai.AiEngine
import com.chriscartland.batterybutler.domain.model.ai.AiMessage
import com.chriscartland.batterybutler.domain.model.ai.AiRole
import com.chriscartland.batterybutler.domain.model.ai.ToolHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Fake [AiEngine] for testing AI-related use cases.
 *
 * Records prompts passed to [generateResponse] and returns configurable responses.
 * By default returns a single [AiMessage] with [defaultResponseText].
 *
 * Example usage:
 * ```kotlin
 * val engine = FakeAiEngine()
 * engine.defaultResponseText = "smoke_detector"
 * val useCase = SuggestDeviceIconUseCase(engine)
 * val result = useCase("Smoke Detector")
 * assertEquals("smoke_detector", result)
 * assertEquals(1, engine.recordedPrompts.size)
 * ```
 */
class FakeAiEngine : AiEngine {
    /** All prompts passed to [generateResponse], in order. */
    val recordedPrompts = mutableListOf<String>()

    /** The text returned in the default response message. */
    var defaultResponseText: String = "AI response"

    /** When set, [generateResponse] will throw this exception instead. */
    var errorToThrow: Exception? = null

    private val _isAvailable = MutableStateFlow(true)
    override val isAvailable: Flow<Boolean> = _isAvailable

    private val _compatibility = MutableStateFlow(true)
    override val compatibility: Flow<Boolean> = _compatibility

    /** Sets the [isAvailable] flow value. */
    fun setAvailable(available: Boolean) {
        _isAvailable.value = available
    }

    /** Sets the [compatibility] flow value. */
    fun setCompatibility(compatible: Boolean) {
        _compatibility.value = compatible
    }

    /** The situation passed with each prompt, when the caller gave one; "" otherwise. */
    val recordedSituations = mutableListOf<String>()

    /** Tool calls to make, through the given handler, before answering. Results are recorded. */
    var scriptedToolCalls: List<Pair<String, Map<String, Any?>>> = emptyList()
    val recordedToolResults = mutableListOf<String>()

    /** Called once [scriptedToolCalls] have run, before answering: the middle of a turn. */
    var afterToolCalls: (() -> Unit)? = null

    /** Thrown after [scriptedToolCalls] have run, like a failure partway through a turn. */
    var errorAfterToolCalls: Exception? = null

    /** Answer with this as an error message, the way the cloud engine reports a failed turn. */
    var errorReplyText: String? = null

    var resetCount = 0
        private set

    override suspend fun generateResponse(
        prompt: String,
        toolHandler: ToolHandler?,
    ): Flow<AiMessage> = generateResponse(prompt, toolHandler) { "" }

    override suspend fun generateResponse(
        prompt: String,
        toolHandler: ToolHandler?,
        situation: suspend () -> String,
    ): Flow<AiMessage> {
        recordedPrompts.add(prompt)
        recordedSituations.add(situation())
        errorToThrow?.let { throw it }
        for ((name, args) in scriptedToolCalls) {
            recordedToolResults += toolHandler?.execute(name, args) ?: "no handler"
        }
        afterToolCalls?.invoke()
        errorAfterToolCalls?.let { throw it }
        errorReplyText?.let {
            return flowOf(AiMessage(id = "fake-error-${recordedPrompts.size}", role = AiRole.MODEL, text = it, isError = true))
        }
        return flowOf(
            AiMessage(
                id = "fake-${recordedPrompts.size}",
                role = AiRole.MODEL,
                text = defaultResponseText,
            ),
        )
    }

    override suspend fun resetConversation() {
        resetCount++
    }
}
