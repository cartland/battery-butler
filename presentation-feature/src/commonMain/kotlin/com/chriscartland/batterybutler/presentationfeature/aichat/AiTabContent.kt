package com.chriscartland.batterybutler.presentationfeature.aichat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.content_desc_send_message
import com.chriscartland.batterybutler.composeresources.generated.resources.placeholder_type_message
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.Padding

/**
 * AI chat content designed to be embedded in a tab layout (within MainScreenShell).
 * Unlike [AiChatContent], this does not include its own Scaffold or top bar.
 */
@Composable
fun AiTabContent(
    messages: List<ChatMessage>,
    isProcessing: Boolean,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    showInput: Boolean = true,
    pendingAction: ChatPendingAction? = null,
    onConfirmPending: () -> Unit = {},
    onCancelPending: () -> Unit = {},
    onUndo: (String) -> Unit = {},
    onRetry: () -> Unit = {},
) {
    var inputText by rememberSaveable { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        ChatTranscript(
            messages = messages,
            isProcessing = isProcessing,
            onSuggestion = onSendMessage,
            pendingAction = pendingAction,
            onConfirmPending = onConfirmPending,
            onCancelPending = onCancelPending,
            onUndo = onUndo,
            onRetry = onRetry,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            modifier = Modifier.weight(1f),
        )

        // Input bar
        if (showInput) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing
                            .only(WindowInsetsSides.Horizontal)
                            .union(WindowInsets.ime),
                    ).padding(horizontal = Padding.standard, vertical = Padding.small),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(composeStringResource(Res.string.placeholder_type_message)) },
                    maxLines = 4,
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(24.dp),
                )
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
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
    }
}

@Composable
fun AiTabContentPreview() {
    BatteryButlerTheme {
        AiTabContent(
            messages = listOf(
                ChatMessage("1", "Add a smoke detector in the kitchen", isUser = true),
                ChatMessage(
                    "2",
                    "Done. I've added it as a smoke detector in the kitchen.",
                    isUser = false,
                    actions = listOf(
                        ChatAction("a1", ChatActionKind.ADDED, "Added Kitchen Smoke Detector (Smoke Detector, Kitchen)", undoable = true),
                    ),
                ),
                ChatMessage("3", "Remove the old garage sensor", isUser = true),
                ChatMessage("4", "The app is asking you to confirm deleting the garage sensor.", isUser = false),
            ),
            isProcessing = false,
            onSendMessage = {},
            pendingAction = ChatPendingAction("p1", "Delete Garage Sensor and its 2 battery changes", "Its photo is removed for good."),
        )
    }
}

@Composable
fun AiTabContentEmptyPreview() {
    BatteryButlerTheme {
        AiTabContent(
            messages = emptyList(),
            isProcessing = false,
            onSendMessage = {},
        )
    }
}
