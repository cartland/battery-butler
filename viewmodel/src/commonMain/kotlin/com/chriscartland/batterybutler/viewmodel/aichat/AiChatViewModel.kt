package com.chriscartland.batterybutler.viewmodel.aichat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.ai.AiMessage
import com.chriscartland.batterybutler.ai.AiRole
import com.chriscartland.batterybutler.usecase.SendChatMessageUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import kotlin.coroutines.cancellation.CancellationException

@Inject
class AiChatViewModel(
    private val sendChatMessageUseCase: SendChatMessageUseCase,
) : ViewModel() {
    private val _messages = MutableStateFlow<List<AiMessage>>(emptyList())
    val messages: StateFlow<List<AiMessage>> = _messages.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private var currentJob: Job? = null

    fun sendMessage(text: String) {
        if (text.isBlank() || _isProcessing.value) return

        val userMessage = AiMessage(
            id = "user_${System.currentTimeMillis()}",
            role = AiRole.USER,
            text = text,
        )
        _messages.update { it + userMessage }
        _isProcessing.value = true

        currentJob = viewModelScope.launch {
            try {
                sendChatMessageUseCase(text).collect { aiMessage ->
                    // Replace partial messages with the final one
                    _messages.update { current ->
                        val withoutPartial = current.filter { !it.isPartial }
                        withoutPartial + aiMessage
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val errorMessage = AiMessage(
                    id = "error_${System.currentTimeMillis()}",
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
