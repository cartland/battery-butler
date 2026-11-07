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
    val pendingAction by viewModel.pendingAction.collectAsStateWithLifecycle()

    AiChatContent(
        messages = messages.map { it.toChatMessage() },
        isProcessing = isProcessing,
        onSendMessage = viewModel::sendMessage,
        onClearChat = viewModel::clearChat,
        onBack = onBack,
        modifier = modifier,
        pendingAction = pendingAction?.toChatPendingAction(),
        onConfirmPending = viewModel::confirmPendingAction,
        onCancelPending = viewModel::cancelPendingAction,
        onUndo = viewModel::undoAction,
        onRetry = viewModel::retryLastMessage,
    )
}
