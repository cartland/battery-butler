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
        _messages.update { it + userMessage }
        _isProcessing.value = true

        val hintLines = hints.entries.joinToString("\n") { "[${it.key}: ${it.value}]" }
        val augmentedText = if (hintLines.isNotEmpty()) "$hintLines\n\n$text" else text

        currentJob = viewModelScope.coroutineScope.launch {
            try {
                sendChatMessageUseCase(augmentedText).collect { aiMessage ->
                    // Replace partial messages with the final one
                    _messages.update { current ->
                        val withoutPartial = current.filter { !it.isPartial }
                        withoutPartial + aiMessage
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
                    withoutPartial + errorMessage
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun clearChat() {
        currentJob?.cancel()
        _messages.value = emptyList()
        _isProcessing.value = false
    }
}
