package com.chriscartland.batterybutler.composeapp.feature.aichat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chriscartland.batterybutler.ai.AiRole
import com.chriscartland.batterybutler.presentationfeature.aichat.AiChatContent
import com.chriscartland.batterybutler.presentationfeature.aichat.ChatUiMessage
import com.chriscartland.batterybutler.viewmodel.aichat.AiChatViewModel

@Composable
fun AiChatScreen(
    viewModel: AiChatViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()

    val uiMessages = messages.map { msg ->
        ChatUiMessage(
            id = msg.id,
            text = msg.text,
            isUser = msg.role == AiRole.USER,
        )
    }

    AiChatContent(
        messages = uiMessages,
        isProcessing = isProcessing,
        onSendMessage = viewModel::sendMessage,
        onClearChat = viewModel::clearChat,
        onBack = onBack,
        modifier = modifier,
    )
}
