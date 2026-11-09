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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_card_approve
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_card_decline
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_card_delete
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_card_status_applied
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_card_status_applying
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_card_status_declined
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_card_status_failed
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_card_status_partly_applied
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_card_status_partly_undone
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_card_status_pending
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_card_status_undoing
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_card_status_undone
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_card_status_withdrawn
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_card_undo
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_chat_empty_title
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_retry
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_suggestion_due
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_suggestion_never_recorded
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_suggestion_recent
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_suggestion_shopping
import com.chriscartland.batterybutler.composeresources.generated.resources.content_desc_ai_thinking
import com.chriscartland.batterybutler.presentationcore.theme.Padding
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import org.jetbrains.compose.resources.StringResource

/**
 * The transcript, shared by the split-screen panel and the full-screen chat.
 *
 * What it shows beyond bubbles, and why:
 * - A card for every change the assistant wants to make. Nothing happens until the person approves
 *   it, and the app wrote every word on it, so a confused model can't misdescribe a change. The
 *   same card then carries the result, the reason for anything that failed, and Undo.
 * - A quiet line saying what the assistant looked at.
 * - A failed reply ends in "Try again" instead of leaving a dead end.
 * - An empty chat offers a few things to ask, so the first message isn't a blank page. A chip
 *   sends at once, so every suggestion is a question.
 */
@Composable
fun ChatTranscript(
    messages: List<ChatMessage>,
    isProcessing: Boolean,
    onSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier,
    onApproveCard: (String, Set<String>) -> Unit = { _, _ -> },
    onDeclineCard: (String) -> Unit = {},
    onUndoCard: (String) -> Unit = {},
    onRetry: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    listState: LazyListState = rememberLazyListState(),
) {
    val rowCount = messages.size + (if (isProcessing) 1 else 0)
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

    if (messages.isEmpty() && !isProcessing) {
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start,
            ) {
                if (message.text.isNotBlank()) ChatBubble(message)
                if (message.lookups.isNotEmpty()) {
                    Text(
                        text = message.lookups.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 2.dp).testTag("ai_lookups"),
                    )
                }
                message.card?.let { card ->
                    ProposalCard(
                        card = card,
                        onApprove = { leftOut -> onApproveCard(card.id, leftOut) },
                        onDecline = { onDeclineCard(card.id) },
                        onUndo = { onUndoCard(card.id) },
                    )
                }
                if (message.isError && isLast && !isProcessing && message.card == null) {
                    TextButton(onClick = onRetry, modifier = Modifier.testTag("ai_retry")) {
                        Text(composeStringResource(Res.string.ai_retry))
                    }
                }
            }
        }
        if (isProcessing) {
            item(key = "thinking") { ThinkingIndicator() }
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

/**
 * Proposed changes and what became of them. The confirm button names the act when everything on
 * the card is a deletion, and a batch can be trimmed item by item before approving.
 */
@Composable
private fun ProposalCard(
    card: ChatCard,
    onApprove: (Set<String>) -> Unit,
    onDecline: () -> Unit,
    onUndo: () -> Unit,
) {
    var leftOut by rememberSaveable(card.id) { mutableStateOf(listOf<String>()) }
    val pending = card.status == ChatCardStatus.PENDING
    val settled = card.status == ChatCardStatus.DECLINED || card.status == ChatCardStatus.WITHDRAWN
    val allDeletes = card.items.all { it.kind == ChatChangeKind.DELETE }
    val busy = card.status == ChatCardStatus.APPLYING || card.status == ChatCardStatus.UNDOING
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (pending) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .testTag("ai_card"),
    ) {
        Column(modifier = Modifier.padding(Padding.medium)) {
            Text(
                text = composeStringResource(card.status.label()),
                style = MaterialTheme.typography.labelMedium,
                color = if (pending) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("ai_card_status"),
            )
            card.items.forEach { item ->
                CardItemRow(
                    item = item,
                    faded = settled || item.outcome == ChatItemOutcome.UNDONE || item.id in leftOut || item.outcome == ChatItemOutcome.LEFT_OUT,
                    checked = (item.id !in leftOut).takeIf { pending && card.items.size > 1 },
                    onCheckedChange = { include -> leftOut = if (include) leftOut - item.id else leftOut + item.id },
                )
            }
            if (pending) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Padding.small),
                    horizontalArrangement = Arrangement.spacedBy(Padding.small, Alignment.End),
                ) {
                    OutlinedButton(onClick = onDecline, modifier = Modifier.testTag("ai_card_decline")) {
                        Text(composeStringResource(Res.string.ai_card_decline))
                    }
                    Button(
                        onClick = { onApprove(leftOut.toSet()) },
                        enabled = leftOut.size < card.items.size,
                        colors = if (allDeletes) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        },
                        modifier = Modifier.testTag("ai_card_approve"),
                    ) {
                        Text(composeStringResource(if (allDeletes) Res.string.ai_card_delete else Res.string.ai_card_approve))
                    }
                }
            }
            if (card.canUndo && !busy) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onUndo, modifier = Modifier.testTag("ai_card_undo")) {
                        Text(composeStringResource(Res.string.ai_card_undo))
                    }
                }
            }
        }
    }
}

@Composable
private fun CardItemRow(
    item: ChatCardItem,
    faded: Boolean,
    checked: Boolean?,
    onCheckedChange: (Boolean) -> Unit,
) {
    val color = if (faded) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Padding.small)
            .testTag("ai_card_item"),
        verticalAlignment = Alignment.Top,
    ) {
        if (checked != null) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.size(24.dp).testTag("ai_card_item_check"),
            )
        } else {
            Icon(
                imageVector = item.kind.icon(),
                contentDescription = null,
                tint = if (faded) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = Padding.small),
        ) {
            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                textDecoration = if (item.outcome == ChatItemOutcome.UNDONE) TextDecoration.LineThrough else null,
            )
            item.warning?.takeIf { item.outcome == ChatItemOutcome.PROPOSED }?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            item.reason?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("ai_card_item_reason"),
                )
            }
        }
    }
}

private fun ChatChangeKind.icon(): ImageVector =
    when (this) {
        ChatChangeKind.ADD -> Icons.Default.Add
        ChatChangeKind.UPDATE -> Icons.Default.Edit
        ChatChangeKind.RECORD -> Icons.Default.Check
        ChatChangeKind.DELETE -> Icons.Default.Delete
        ChatChangeKind.MARK -> Icons.Default.Warning
    }

private fun ChatCardStatus.label(): StringResource =
    when (this) {
        ChatCardStatus.PENDING -> Res.string.ai_card_status_pending
        ChatCardStatus.APPLYING -> Res.string.ai_card_status_applying
        ChatCardStatus.APPLIED -> Res.string.ai_card_status_applied
        ChatCardStatus.PARTLY_APPLIED -> Res.string.ai_card_status_partly_applied
        ChatCardStatus.FAILED -> Res.string.ai_card_status_failed
        ChatCardStatus.DECLINED -> Res.string.ai_card_status_declined
        ChatCardStatus.WITHDRAWN -> Res.string.ai_card_status_withdrawn
        ChatCardStatus.UNDOING -> Res.string.ai_card_status_undoing
        ChatCardStatus.UNDONE -> Res.string.ai_card_status_undone
        ChatCardStatus.PARTLY_UNDONE -> Res.string.ai_card_status_partly_undone
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
        composeStringResource(Res.string.ai_suggestion_recent),
        composeStringResource(Res.string.ai_suggestion_never_recorded),
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
