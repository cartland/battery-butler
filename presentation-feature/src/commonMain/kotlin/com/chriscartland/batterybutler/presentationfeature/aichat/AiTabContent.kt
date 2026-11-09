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
    onApproveCard: (String, Set<String>) -> Unit = { _, _ -> },
    onDeclineCard: (String) -> Unit = {},
    onUndoCard: (String) -> Unit = {},
    onRetry: () -> Unit = {},
) {
    var inputText by rememberSaveable { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        ChatTranscript(
            messages = messages,
            isProcessing = isProcessing,
            onSuggestion = onSendMessage,
            onApproveCard = onApproveCard,
            onDeclineCard = onDeclineCard,
            onUndoCard = onUndoCard,
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
                    "",
                    isUser = false,
                    card = ChatCard(
                        id = "c1",
                        items = listOf(ChatCardItem("i1", ChatChangeKind.ADD, "Add Kitchen Smoke Detector (Smoke Detector, Kitchen)", outcome = ChatItemOutcome.APPLIED)),
                        status = ChatCardStatus.APPLIED,
                        canUndo = true,
                    ),
                ),
                ChatMessage("3", "Remove the old garage sensor", isUser = true),
                ChatMessage(
                    "4",
                    "",
                    isUser = false,
                    card = ChatCard(
                        id = "c2",
                        items = listOf(ChatCardItem("i2", ChatChangeKind.DELETE, "Delete Garage Sensor and its 2 battery changes", warning = "Undo can't bring back its photo.")),
                        status = ChatCardStatus.PENDING,
                    ),
                ),
            ),
            isProcessing = false,
            onSendMessage = {},
        )
    }
}

@Composable
fun AiTabContentCardStatesPreview() {
    BatteryButlerTheme {
        AiTabContent(
            messages = listOf(
                ChatMessage(
                    id = "m1",
                    text = "",
                    isUser = false,
                    lookups = listOf("Listed devices", "Looked up the history of Hall Alarm"),
                    card = ChatCard(
                        id = "c1",
                        items = listOf(
                            ChatCardItem("a", ChatChangeKind.RECORD, "Record a battery change for Hall Alarm on 2026-11-09"),
                            ChatCardItem("b", ChatChangeKind.UPDATE, "Update Hall Alarm: location Hall → Landing"),
                            ChatCardItem("c", ChatChangeKind.RECORD, "Record a battery change for Hal Alarm on 2026-11-09", warning = "There's no device called Hal Alarm yet, so this also adds one."),
                        ),
                        status = ChatCardStatus.PENDING,
                    ),
                ),
                ChatMessage(
                    id = "m2",
                    text = "",
                    isUser = false,
                    card = ChatCard(
                        id = "c2",
                        items = listOf(ChatCardItem("d", ChatChangeKind.DELETE, "Delete Garage Sensor and its 2 battery changes", warning = "Undo can't bring back its photo.")),
                        status = ChatCardStatus.PENDING,
                    ),
                ),
                ChatMessage(
                    id = "m3",
                    text = "",
                    isUser = false,
                    card = ChatCard(
                        id = "c3",
                        items = listOf(
                            ChatCardItem("e", ChatChangeKind.ADD, "Add Porch Light", outcome = ChatItemOutcome.APPLIED),
                            ChatCardItem("f", ChatChangeKind.UPDATE, "Update Gate Sensor: name Gate → Side Gate", outcome = ChatItemOutcome.CHANGED_SINCE, reason = "It changed after this was proposed, so it was left alone."),
                        ),
                        status = ChatCardStatus.PARTLY_APPLIED,
                        canUndo = true,
                    ),
                ),
                ChatMessage(
                    id = "m4",
                    text = "",
                    isUser = false,
                    card = ChatCard(
                        id = "c4",
                        items = listOf(ChatCardItem("g", ChatChangeKind.ADD, "Add Hall Alarm", outcome = ChatItemOutcome.UNDO_FAILED, reason = "Hall Alarm has battery changes recorded since, so adding it can't be undone. Delete it instead.")),
                        status = ChatCardStatus.PARTLY_UNDONE,
                        canUndo = true,
                    ),
                ),
                ChatMessage(
                    id = "m5",
                    text = "",
                    isUser = false,
                    card = ChatCard(
                        id = "c5",
                        items = listOf(ChatCardItem("h", ChatChangeKind.RECORD, "Record a battery change for Attic Alarm on 2026-11-09", outcome = ChatItemOutcome.UNDONE)),
                        status = ChatCardStatus.UNDONE,
                    ),
                ),
                ChatMessage(
                    id = "m6",
                    text = "",
                    isUser = false,
                    card = ChatCard(
                        id = "c6",
                        items = listOf(ChatCardItem("i", ChatChangeKind.DELETE, "Delete the type Remote")),
                        status = ChatCardStatus.DECLINED,
                    ),
                ),
            ),
            isProcessing = false,
            onSendMessage = {},
            showInput = false,
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
