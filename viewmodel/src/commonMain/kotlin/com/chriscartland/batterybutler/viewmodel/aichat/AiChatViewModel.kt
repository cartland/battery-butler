package com.chriscartland.batterybutler.viewmodel.aichat

import com.chriscartland.batterybutler.domain.model.ai.AiMessage
import com.chriscartland.batterybutler.domain.model.ai.AiRole
import com.chriscartland.batterybutler.usecase.SendChatMessageUseCase
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

@Inject
class AiChatViewModel(
    private val sendChatMessageUseCase: SendChatMessageUseCase,
) : ViewModel() {
    private val _messages = MutableStateFlow<List<AiMessage>>(viewModelScope, emptyList())
    val messages: StateFlow<List<AiMessage>> = _messages.asStateFlow()

    private val _isProcessing = MutableStateFlow(viewModelScope, false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private var currentJob: Job? = null

    fun sendMessage(
        text: String,
        hints: Map<String, String> = emptyMap(),
    ) {
        if (text.isBlank() || _isProcessing.value) return

        val userMessage = AiMessage(
            id = "user_${Clock.System.now().toEpochMilliseconds()}",
            role = AiRole.USER,
            text = text,
            hints = hints,
        )
        _messages.update { it.plusUnique(userMessage) }
        _isProcessing.value = true

        val hintLines = hints.entries.joinToString("\n") { "[${it.key}: ${it.value}]" }
        val augmentedText = if (hintLines.isNotEmpty()) "$hintLines\n\n$text" else text

        currentJob = viewModelScope.coroutineScope.launch {
            try {
                sendChatMessageUseCase(augmentedText).collect { aiMessage ->
                    // Replace partial messages with the final one
                    _messages.update { current ->
                        val withoutPartial = current.filter { !it.isPartial }
                        withoutPartial.plusUnique(aiMessage)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val errorMessage = AiMessage(
                    id = "error_${Clock.System.now().toEpochMilliseconds()}",
                    role = AiRole.MODEL,
                    text = "Error: ${e.message}",
                )
                _messages.update { current ->
                    val withoutPartial = current.filter { !it.isPartial }
                    withoutPartial.plusUnique(errorMessage)
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Appends [message], remapping its id if the list already contains it.
     *
     * The chat UIs key list items by message id (Compose `LazyColumn(key = ...)` crashes the app
     * on a duplicate key), but ids come from platform [AiEngine][com.chriscartland.batterybutler.domain.model.ai.AiEngine]
     * implementations that have shipped fixed ids (e.g. every error emitted as `"error"`), and the
     * timestamp-based ids above can collide within one millisecond. This list's owner is the last
     * line of defense, so uniqueness is enforced here rather than trusted from below.
     */
    private fun List<AiMessage>.plusUnique(message: AiMessage): List<AiMessage> {
        val ids = mapTo(HashSet()) { it.id }
        if (message.id !in ids) return this + message
        var suffix = 2
        while ("${message.id}_$suffix" in ids) suffix++
        return this + message.copy(id = "${message.id}_$suffix")
    }

    fun clearChat() {
        currentJob?.cancel()
        _messages.value = emptyList()
        _isProcessing.value = false
    }
}
