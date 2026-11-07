package com.chriscartland.batterybutler.viewmodel.aichat

import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.ai.AiMessage
import com.chriscartland.batterybutler.domain.model.ai.AiPendingAction
import com.chriscartland.batterybutler.domain.model.ai.AiRole
import com.chriscartland.batterybutler.usecase.AiChatSession
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

/**
 * The chat transcript and the controls around it. The rules about what the assistant may do
 * live in [AiChatSession]; this only turns its answers into messages.
 */
@Inject
class AiChatViewModel(
    private val session: AiChatSession,
) : ViewModel() {
    private val _messages = MutableStateFlow<List<AiMessage>>(viewModelScope, emptyList())
    val messages: StateFlow<List<AiMessage>> = _messages.asStateFlow()

    private val _isProcessing = MutableStateFlow(viewModelScope, false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    /** The deletion waiting for the person's answer, if any. Shown as a card under the chat. */
    private val _pendingAction = MutableStateFlow<AiPendingAction?>(viewModelScope, null)
    val pendingAction: StateFlow<AiPendingAction?> = _pendingAction.asStateFlow()

    private var currentJob: Job? = null
    private var lastSend: Pair<String, Map<String, String>>? = null

    init {
        viewModelScope.coroutineScope.launch {
            session.pendingAction.collect { _pendingAction.value = it }
        }
    }

    fun sendMessage(
        text: String,
        hints: Map<String, String> = emptyMap(),
    ) {
        if (text.isBlank() || _isProcessing.value) return

        val userMessage = AiMessage(id = "user_${now()}", role = AiRole.USER, text = text, hints = hints)
        _messages.update { it + userMessage }
        _isProcessing.value = true
        lastSend = text to hints

        currentJob = viewModelScope.coroutineScope.launch {
            try {
                session.send(text, hints).collect { aiMessage ->
                    // Replace partial messages with the final one
                    _messages.update { current -> current.filter { !it.isPartial } + aiMessage }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _messages.update { current ->
                    current.filter { !it.isPartial } +
                        AiMessage(id = "error_${now()}", role = AiRole.MODEL, text = FAILED_MESSAGE, isError = true)
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /** Send the last message again, dropping the error it ended in. */
    fun retryLastMessage() {
        val (text, hints) = lastSend ?: return
        if (_isProcessing.value) return
        _messages.update { current ->
            // The message being retried is still the last user turn; the error after it goes.
            val lastUser = current.indexOfLast { it.role == AiRole.USER }
            if (lastUser >= 0) current.take(lastUser) else current
        }
        sendMessage(text, hints)
    }

    /** The person agreed to the pending deletion. What happened is appended to the transcript. */
    fun confirmPendingAction() {
        viewModelScope.coroutineScope.launch {
            val outcome = session.confirmPendingAction() ?: return@launch
            val action = outcome.action
            _messages.update {
                it + AiMessage(
                    id = "system_${now()}",
                    role = AiRole.SYSTEM,
                    text = if (action == null) outcome.modelResult else "",
                    isError = action == null,
                    actions = listOfNotNull(action),
                )
            }
        }
    }

    fun cancelPendingAction() {
        session.cancelPendingAction()
    }

    /** Take back one action. The row it came from shows "undone"; the model is told next turn. */
    fun undoAction(actionId: String) {
        viewModelScope.coroutineScope.launch {
            when (val result = session.undo(actionId)) {
                is Result.Success -> _messages.update { current ->
                    current.map { message ->
                        message.copy(actions = message.actions.map { if (it.id == actionId) it.copy(undone = true, undoable = false) else it })
                    }
                }

                is Result.Error -> _messages.update {
                    it + AiMessage(id = "error_${now()}", role = AiRole.SYSTEM, text = result.error.message, isError = true)
                }
            }
        }
    }

    fun clearChat() {
        currentJob?.cancel()
        _messages.value = emptyList()
        _isProcessing.value = false
        lastSend = null
        viewModelScope.coroutineScope.launch { session.reset() }
    }

    private fun now() = Clock.System.now().toEpochMilliseconds()

    private companion object {
        const val FAILED_MESSAGE = "Something went wrong and the assistant couldn't answer. Nothing in your data was changed by this message."
    }
}
