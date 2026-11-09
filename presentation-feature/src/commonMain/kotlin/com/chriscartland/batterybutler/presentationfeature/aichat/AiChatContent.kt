package com.chriscartland.batterybutler.presentationfeature.aichat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.action_clear_chat
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_assistant_title
import com.chriscartland.batterybutler.composeresources.generated.resources.content_desc_send_message
import com.chriscartland.batterybutler.composeresources.generated.resources.placeholder_type_message
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.Padding

@Composable
fun AiChatContent(
    messages: List<ChatMessage>,
    isProcessing: Boolean,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onApproveCard: (String, Set<String>) -> Unit = { _, _ -> },
    onDeclineCard: (String) -> Unit = {},
    onUndoCard: (String) -> Unit = {},
    onRetry: () -> Unit = {},
) {
    var inputText by rememberSaveable { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        // Handle bottom inset manually in ChatInputBar to coordinate with IME
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
        topBar = {
            ButlerCenteredTopAppBar(
                title = composeStringResource(Res.string.ai_assistant_title),
                onBack = onBack,
                actions = {
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = onClearChat) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = composeStringResource(Res.string.action_clear_chat),
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            ChatInputBar(
                inputText = inputText,
                onInputChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText)
                        inputText = ""
                    }
                },
                isProcessing = isProcessing,
                modifier = Modifier.windowInsetsPadding(
                    // Horizontal safe drawing (sides)
                    // + Union of IME and Navigation Bars for smooth bottom handling
                    WindowInsets.safeDrawing
                        .only(WindowInsetsSides.Horizontal)
                        .union(WindowInsets.ime.union(WindowInsets.navigationBars)),
                ),
            )
        },
    ) { innerPadding ->
        ChatTranscript(
            messages = messages,
            isProcessing = isProcessing,
            onSuggestion = onSendMessage,
            onApproveCard = onApproveCard,
            onDeclineCard = onDeclineCard,
            onUndoCard = onUndoCard,
            onRetry = onRetry,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isProcessing: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Padding.standard, vertical = Padding.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(composeStringResource(Res.string.placeholder_type_message)) },
            maxLines = 4,
            enabled = !isProcessing,
            shape = RoundedCornerShape(24.dp),
        )
        IconButton(
            onClick = onSend,
            enabled = inputText.isNotBlank() && !isProcessing,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = composeStringResource(Res.string.content_desc_send_message),
                tint = if (inputText.isNotBlank() && !isProcessing) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AiChatContentPreview() {
    BatteryButlerTheme {
        AiChatContent(
            messages = listOf(
                ChatMessage("1", "Add a smoke detector in the kitchen", isUser = true),
                ChatMessage(
                    "2",
                    "I've added a smoke detector device in the kitchen for you. " +
                        "I also created a \"Smoke Detector\" device type.",
                    isUser = false,
                ),
                ChatMessage("3", "Record a battery replacement for it today", isUser = true),
                ChatMessage(
                    "4",
                    "Done! I've recorded a battery replacement for the kitchen smoke detector.",
                    isUser = false,
                ),
            ),
            isProcessing = false,
            onSendMessage = {},
            onClearChat = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AiChatContentEmptyPreview() {
    BatteryButlerTheme {
        AiChatContent(
            messages = emptyList(),
            isProcessing = false,
            onSendMessage = {},
            onClearChat = {},
            onBack = {},
        )
    }
}
