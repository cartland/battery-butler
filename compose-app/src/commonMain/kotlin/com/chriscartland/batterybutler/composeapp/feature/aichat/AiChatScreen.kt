package com.chriscartland.batterybutler.composeapp.feature.aichat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chriscartland.batterybutler.presentationfeature.aichat.AiChatContent
import com.chriscartland.batterybutler.viewmodel.aichat.AiChatViewModel

@Composable
fun AiChatScreen(
    viewModel: AiChatViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val cards by viewModel.cards.collectAsStateWithLifecycle()

    AiChatContent(
        messages = messages.map { it.toChatMessage(cards) },
        isProcessing = isProcessing,
        onSendMessage = viewModel::sendMessage,
        onClearChat = viewModel::clearChat,
        onBack = onBack,
        modifier = modifier,
        onApproveCard = viewModel::approveCard,
        onDeclineCard = viewModel::declineCard,
        onUndoCard = viewModel::undoCard,
        onRetry = viewModel::retryLastMessage,
    )
}
