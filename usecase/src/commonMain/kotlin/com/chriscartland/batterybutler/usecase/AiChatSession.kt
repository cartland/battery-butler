package com.chriscartland.batterybutler.usecase

import com.benasher44.uuid.uuid4
import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.ai.AiAction
import com.chriscartland.batterybutler.domain.model.ai.AiEngine
import com.chriscartland.batterybutler.domain.model.ai.AiMessage
import com.chriscartland.batterybutler.domain.model.ai.AiPendingAction
import com.chriscartland.batterybutler.domain.model.ai.AiToolNames
import com.chriscartland.batterybutler.domain.model.ai.ToolHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

/**
 * One conversation with the assistant, and everything the app holds on its behalf: the deletion
 * waiting for the person's answer, the way back from each change, and what to tell the model
 * about outcomes it didn't see.
 *
 * Three rules, kept here rather than in the prompt:
 *
 * - **A deletion never runs on the model's say-so.** The tool call becomes an [AiPendingAction];
 *   the model is told to describe it and stop; the person decides on a card. Only one can wait
 *   at a time, and a new message withdraws it.
 * - **Everything else is undoable**, so it runs at once. The [AiAction] the person sees is
 *   written by the tool from the stored data, and its undo stays available until the chat is
 *   cleared.
 * - **The model is told what happened.** A confirmation, a refusal or an undo is something it
 *   didn't do and can't see, so the next message carries a bracketed note about it, and the
 *   prompt tells the model to trust those notes over its own memory.
 */
@Inject
class AiChatSession(
    private val aiEngine: AiEngine,
    private val toolHandler: DeviceToolHandler,
    private val buildAiContextUseCase: BuildAiContextUseCase,
) {
    private val _pendingAction = MutableStateFlow<AiPendingAction?>(null)
    val pendingAction: StateFlow<AiPendingAction?> = _pendingAction.asStateFlow()

    private var pendingRun: (suspend () -> ToolOutcome)? = null
    private val undos = mutableMapOf<String, Undo>()
    private val notes = mutableListOf<String>()

    private class Undo(
        val summary: String,
        val run: suspend () -> Result<Unit, DataError>,
    )

    /**
     * Send one message. The returned messages carry the actions taken while answering it.
     * [hints] are facts about where the person is in the app, shown to the model, not to them.
     */
    suspend fun send(
        text: String,
        hints: Map<String, String> = emptyMap(),
    ): Flow<AiMessage> {
        _pendingAction.value?.let { withdraw(it) }
        val prompt = buildString {
            notes.forEach { appendLine("[$it]") }
            hints.forEach { (k, v) -> appendLine("[$k: $v]") }
            if (isNotEmpty()) appendLine()
            append(text)
        }
        notes.clear()

        val turnActions = mutableListOf<AiAction>()
        val recording = ToolHandler { name, args ->
            if (name in AiToolNames.DESTRUCTIVE) {
                defer(name, args)
            } else {
                val outcome = toolHandler.run(name, args)
                outcome.action?.let { action ->
                    turnActions += action
                    outcome.undo?.let { undos[action.id] = Undo(action.summary, it) }
                }
                outcome.modelResult
            }
        }
        return aiEngine
            .generateResponse(prompt, recording) { buildAiContextUseCase() }
            .map { message -> if (message.isPartial) message else message.copy(actions = turnActions.toList()) }
    }

    /** Hold a deletion for the person's answer, and tell the model so. */
    private suspend fun defer(
        name: String,
        args: Map<String, Any?>,
    ): String {
        val preview = toolHandler.describe(name, args) ?: return toolHandler.run(name, args).modelResult
        if (pendingRun != null) {
            return "Error: another deletion is already waiting for the user's answer. Ask them to decide on it first."
        }
        _pendingAction.value = AiPendingAction(id = "pending_${uuid4()}", summary = preview.summary, consequence = preview.consequence)
        pendingRun = { toolHandler.run(name, args) }
        return "Waiting for the user: the app is showing them a card to confirm \"${preview.summary}\". " +
            "Do not call this tool again. Tell the user in one sentence what the card will do, then stop."
    }

    /** The person said yes. Runs the deletion; the outcome is the message to show them. */
    suspend fun confirmPendingAction(): ToolOutcome? {
        val run = pendingRun ?: return null
        val pending = _pendingAction.value
        pendingRun = null
        _pendingAction.value = null
        val outcome = run()
        val action = outcome.action
        if (action != null) {
            outcome.undo?.let { undos[action.id] = Undo(action.summary, it) }
            notes += "The user confirmed, and the app did this: ${action.summary}."
        } else {
            notes += "The user confirmed \"${pending?.summary}\" but it failed: ${outcome.modelResult}"
        }
        return outcome
    }

    /** The person said no. Nothing happens, and the model is told. */
    fun cancelPendingAction(): AiPendingAction? {
        val pending = _pendingAction.value ?: return null
        pendingRun = null
        _pendingAction.value = null
        notes += "The user declined \"${pending.summary}\". Nothing was deleted."
        return pending
    }

    private fun withdraw(pending: AiPendingAction) {
        pendingRun = null
        _pendingAction.value = null
        notes += "The user sent a new message instead of confirming \"${pending.summary}\". It was not done."
    }

    /** Take back an action. Afterwards the model is told, so it won't describe the change as done. */
    suspend fun undo(actionId: String): Result<Unit, DataError> {
        val undo = undos.remove(actionId) ?: return Result.Error(DataError.Unknown("That can no longer be undone", null))
        return when (val result = undo.run()) {
            is Result.Success -> {
                notes += "The user undid this: ${undo.summary}. Treat it as not having happened."
                result
            }

            is Result.Error -> {
                undos[actionId] = undo
                result
            }
        }
    }

    fun canUndo(actionId: String): Boolean = actionId in undos

    /** Forget everything: the conversation, the pending deletion, and every way back. */
    suspend fun reset() {
        pendingRun = null
        _pendingAction.value = null
        undos.clear()
        notes.clear()
        aiEngine.resetConversation()
    }
}
