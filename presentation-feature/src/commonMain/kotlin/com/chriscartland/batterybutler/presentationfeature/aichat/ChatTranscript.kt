package com.chriscartland.batterybutler.presentationfeature.aichat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_action_undo
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_action_undone
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_chat_empty_title
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_pending_cancel
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_pending_confirm
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_pending_irreversible
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_pending_title
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_retry
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_suggestion_add
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_suggestion_due
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_suggestion_record
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_suggestion_shopping
import com.chriscartland.batterybutler.composeresources.generated.resources.content_desc_ai_thinking
import com.chriscartland.batterybutler.presentationcore.theme.Padding
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

/**
 * The transcript, shared by the split-screen panel and the full-screen chat.
 *
 * What it shows beyond bubbles, and why:
 * - A row under a reply for each change the app made, with Undo. The assistant's words say
 *   what it meant to do; the row says what happened.
 * - A card when a deletion is waiting for the person. Nothing is deleted until they tap it, and
 *   it says what would be lost for good.
 * - A failed reply ends in "Try again" instead of leaving a dead end.
 * - An empty chat offers a few things to ask, so the first message isn't a blank page.
 */
@Composable
fun ChatTranscript(
    messages: List<ChatMessage>,
    isProcessing: Boolean,
    onSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier,
    pendingAction: ChatPendingAction? = null,
    onConfirmPending: () -> Unit = {},
    onCancelPending: () -> Unit = {},
    onUndo: (String) -> Unit = {},
    onRetry: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    listState: LazyListState = rememberLazyListState(),
) {
    // Everything after the messages (the card, the typing dots) counts as one more row to scroll to.
    val rowCount = messages.size + (if (pendingAction != null) 1 else 0) + (if (isProcessing) 1 else 0)
    LaunchedEffect(rowCount) {
        if (rowCount > 0) listState.animateScrollToItem(rowCount - 1)
    }
    // Keep the newest row in view when the viewport changes height (the panel resizing, the keyboard).
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.viewportSize.height }
            .distinctUntilChanged()
            .drop(1)
            .collect { if (rowCount > 0) listState.animateScrollToItem(rowCount - 1) }
    }

    if (messages.isEmpty() && pendingAction == null && !isProcessing) {
        EmptyTranscript(onSuggestion = onSuggestion, modifier = modifier.fillMaxSize())
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(messages, key = { it.id }) { message ->
            val isLast = message.id == messages.last().id
            ChatEntry(
                message = message,
                onUndo = onUndo,
                onRetry = if (message.isError && isLast && !isProcessing) onRetry else null,
            )
        }
        if (pendingAction != null) {
            item(key = pendingAction.id) {
                PendingActionCard(pendingAction, onConfirm = onConfirmPending, onCancel = onCancelPending)
            }
        }
        if (isProcessing) {
            item(key = "thinking") { ThinkingIndicator() }
        }
    }
}

@Composable
private fun ChatEntry(
    message: ChatMessage,
    onUndo: (String) -> Unit,
    onRetry: (() -> Unit)?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start,
    ) {
        if (message.text.isNotBlank()) ChatBubble(message)
        message.actions.forEach { action ->
            ActionRow(action = action, onUndo = { onUndo(action.id) })
        }
        if (onRetry != null) {
            TextButton(onClick = onRetry, modifier = Modifier.testTag("ai_retry")) {
                Text(composeStringResource(Res.string.ai_retry))
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val containerColor = when {
        message.isError -> MaterialTheme.colorScheme.errorContainer
        message.isUser -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        message.isError -> MaterialTheme.colorScheme.onErrorContainer
        message.isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (message.isUser) 16.dp else 4.dp,
            bottomEnd = if (message.isUser) 4.dp else 16.dp,
        ),
        modifier = Modifier.fillMaxWidth(0.85f),
    ) {
        Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            modifier = Modifier.padding(Padding.medium),
        )
    }
}

/** One change the app made: what, and the way back while there still is one. */
@Composable
private fun ActionRow(
    action: ChatAction,
    onUndo: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .padding(top = 2.dp)
            .testTag("ai_action"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = action.kind.icon(),
            contentDescription = null,
            tint = if (action.undone) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = action.summary,
            style = MaterialTheme.typography.bodySmall,
            color = if (action.undone) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (action.undone) TextDecoration.LineThrough else null,
            modifier = Modifier
                .weight(1f)
                .padding(start = Padding.small),
        )
        when {
            action.undone -> Text(
                text = composeStringResource(Res.string.ai_action_undone),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
            )

            action.undoable -> TextButton(onClick = onUndo, modifier = Modifier.testTag("ai_undo")) {
                Text(composeStringResource(Res.string.ai_action_undo))
            }
        }
    }
}

private fun ChatActionKind.icon(): ImageVector =
    when (this) {
        ChatActionKind.ADDED -> Icons.Default.Add
        ChatActionKind.UPDATED -> Icons.Default.Edit
        ChatActionKind.RECORDED -> Icons.Default.Check
        ChatActionKind.DELETED -> Icons.Default.Delete
        ChatActionKind.MARKED -> Icons.Default.Warning
        ChatActionKind.LOOKED_UP -> Icons.Default.Search
    }

/**
 * A deletion waiting for the person. The confirm button names the act, and the card says what
 * can't come back, so agreeing is a decision rather than a reflex.
 */
@Composable
private fun PendingActionCard(
    pending: ChatPendingAction,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_pending"),
    ) {
        Column(modifier = Modifier.padding(Padding.medium)) {
            Text(
                text = composeStringResource(Res.string.ai_pending_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                text = pending.summary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(top = Padding.extraSmall),
            )
            Text(
                text = pending.consequence ?: composeStringResource(Res.string.ai_pending_irreversible),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(top = Padding.extraSmall),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Padding.small),
                horizontalArrangement = Arrangement.spacedBy(Padding.small, Alignment.End),
            ) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.testTag("ai_pending_cancel")) {
                    Text(composeStringResource(Res.string.ai_pending_cancel))
                }
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    modifier = Modifier.testTag("ai_pending_confirm"),
                ) {
                    Text(composeStringResource(Res.string.ai_pending_confirm))
                }
            }
        }
    }
}

/** Three dots that take turns, in place of a reply that hasn't come yet. */
@Composable
private fun ThinkingIndicator() {
    val transition = rememberInfiniteTransition(label = "thinking")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val description = composeStringResource(Res.string.content_desc_ai_thinking)
    Row(
        modifier = Modifier
            .padding(Padding.small)
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(3) { index ->
            val lit = phase.toInt() % 3 == index
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(if (lit) 1f else 0.35f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyTranscript(
    onSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val suggestions = listOf(
        composeStringResource(Res.string.ai_suggestion_due),
        composeStringResource(Res.string.ai_suggestion_shopping),
        composeStringResource(Res.string.ai_suggestion_record),
        composeStringResource(Res.string.ai_suggestion_add),
    )
    Column(
        modifier = modifier.padding(Padding.standard),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = composeStringResource(Res.string.ai_chat_empty_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.padding(top = Padding.medium),
            horizontalArrangement = Arrangement.spacedBy(Padding.small, Alignment.CenterHorizontally),
        ) {
            suggestions.forEach { suggestion ->
                SuggestionChip(
                    onClick = { onSuggestion(suggestion) },
                    label = { Text(suggestion) },
                    modifier = Modifier.testTag("ai_suggestion"),
                )
            }
        }
    }
}
