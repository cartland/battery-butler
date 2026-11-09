package com.chriscartland.batterybutler.viewmodel.aichat

import com.chriscartland.batterybutler.domain.model.ai.AiCard
import com.chriscartland.batterybutler.domain.model.ai.AiMessage
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

    /** Cards of proposed changes, by id. Messages point at these; a card changes as it's decided. */
    private val _cards = MutableStateFlow<Map<String, AiCard>>(viewModelScope, emptyMap())
    val cards: StateFlow<Map<String, AiCard>> = _cards.asStateFlow()

    private var currentJob: Job? = null
    private var lastSend: Pair<String, Map<String, String>>? = null

    init {
        viewModelScope.coroutineScope.launch {
            session.cards.collect { _cards.value = it }
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
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /** Send the last message again, dropping the error it ended in. Never when it left a card. */
    fun retryLastMessage() {
        val (text, hints) = lastSend ?: return
        if (_isProcessing.value) return
        if (_messages.value.lastOrNull()?.cardId != null) return
        _messages.update { current ->
            // The message being retried is still the last user turn; the error after it goes.
            val lastUser = current.indexOfLast { it.role == AiRole.USER }
            if (lastUser >= 0) current.take(lastUser) else current
        }
        sendMessage(text, hints)
    }

    fun approveCard(
        cardId: String,
        leftOut: Set<String> = emptySet(),
    ) {
        viewModelScope.coroutineScope.launch { session.approve(cardId, leftOut) }
    }

    fun declineCard(cardId: String) {
        viewModelScope.coroutineScope.launch { session.decline(cardId) }
    }

    fun undoCard(cardId: String) {
        viewModelScope.coroutineScope.launch { session.undo(cardId) }
    }

    fun clearChat() {
        currentJob?.cancel()
        _messages.value = emptyList()
        _isProcessing.value = false
        lastSend = null
        viewModelScope.coroutineScope.launch { session.reset() }
    }

    private fun now() = Clock.System.now().toEpochMilliseconds()
}
