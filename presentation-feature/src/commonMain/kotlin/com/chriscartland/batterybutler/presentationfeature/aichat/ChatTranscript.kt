package com.chriscartland.batterybutler.presentationfeature.aichat

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_thinking
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
    steps: List<String>,
    onSuggestion: (String) -> Unit,
    onApproveCard: (String, Set<String>) -> Unit,
    onDeclineCard: (String) -> Unit,
    onUndoCard: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    listState: LazyListState = rememberLazyListState(),
) {
    val rowCount = messages.size + (if (isProcessing) 1 else 0)
    LaunchedEffect(rowCount, steps.size) {
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
            item(key = "thinking") { ThinkingIndicator(steps) }
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
@OptIn(ExperimentalFoundationApi::class)
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
    val haptics = LocalHapticFeedback.current

    // Whenever this card changes state after it first appears, scroll it fully into view, so the
    // result of a tap is never just off-screen. Not on first appearance: that would fight a scroll.
    val bringIntoView = remember { BringIntoViewRequester() }
    val outcomes = card.items.map { it.outcome }
    var seen by remember(card.id) { mutableStateOf(card.status to outcomes) }
    LaunchedEffect(card.status, outcomes) {
        if (seen != card.status to outcomes) {
            seen = card.status to outcomes
            bringIntoView.bringIntoView()
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (pending) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .bringIntoViewRequester(bringIntoView)
            .testTag("ai_card"),
    ) {
        Column(
            modifier = Modifier
                .padding(Padding.medium)
                .animateContentSize(),
        ) {
            StatusLine(card.status, onContainer = pending)
            card.items.forEach { item ->
                CardItemRow(
                    item = item,
                    faded = settled || item.outcome == ChatItemOutcome.UNDONE || item.id in leftOut || item.outcome == ChatItemOutcome.LEFT_OUT,
                    checked = (item.id !in leftOut).takeIf { pending && card.items.size > 1 },
                    onCheckedChange = { include -> leftOut = if (include) leftOut - item.id else leftOut + item.id },
                )
            }
            if (busy) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Padding.small)
                        .testTag("ai_card_busy"),
                )
            }
            if (pending) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Padding.small),
                    horizontalArrangement = Arrangement.spacedBy(Padding.small, Alignment.End),
                ) {
                    OutlinedButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.Reject)
                            onDecline()
                        },
                        modifier = Modifier.testTag("ai_card_decline"),
                    ) {
                        Text(composeStringResource(Res.string.ai_card_decline))
                    }
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            onApprove(leftOut.toSet())
                        },
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
                    TextButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            onUndo()
                        },
                        modifier = Modifier.testTag("ai_card_undo"),
                    ) {
                        Text(composeStringResource(Res.string.ai_card_undo))
                    }
                }
            }
        }
    }
}

/** The card's state at a glance: an icon and a colour as well as the words. */
@Composable
private fun StatusLine(
    status: ChatCardStatus,
    onContainer: Boolean,
) {
    val (icon, tint) = when (status) {
        ChatCardStatus.APPLIED, ChatCardStatus.UNDONE -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
        ChatCardStatus.PARTLY_APPLIED, ChatCardStatus.FAILED, ChatCardStatus.PARTLY_UNDONE -> Icons.Default.Warning to MaterialTheme.colorScheme.error
        ChatCardStatus.DECLINED, ChatCardStatus.WITHDRAWN -> Icons.Default.Close to MaterialTheme.colorScheme.outline
        ChatCardStatus.PENDING, ChatCardStatus.APPLYING, ChatCardStatus.UNDOING -> null to MaterialTheme.colorScheme.onTertiaryContainer
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("ai_card_status")) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp).padding(end = 2.dp))
        }
        Text(
            text = composeStringResource(status.label()),
            style = MaterialTheme.typography.labelLarge,
            color = when {
                icon != null -> tint
                onContainer -> MaterialTheme.colorScheme.onTertiaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(start = if (icon != null) 4.dp else 0.dp),
        )
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
        ChatChangeKind.MARK -> Icons.Default.Notifications
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

/**
 * What the assistant has done so far, then three dots that take turns. A reply can take several
 * seconds, and a step appearing as each lookup or draft happens shows that it's getting somewhere.
 */
@Composable
private fun ThinkingIndicator(steps: List<String>) {
    val transition = rememberInfiniteTransition(label = "thinking")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val description = composeStringResource(Res.string.content_desc_ai_thinking)
    Column(
        modifier = Modifier
            .padding(Padding.small)
            .animateContentSize()
            .testTag("ai_thinking"),
    ) {
        steps.forEach { step ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 2.dp)) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = step,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 4.dp).testTag("ai_step"),
                )
            }
        }
        Row(
            modifier = Modifier.semantics { contentDescription = description },
            verticalAlignment = Alignment.CenterVertically,
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
            Text(
                text = composeStringResource(Res.string.ai_thinking),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 4.dp),
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
