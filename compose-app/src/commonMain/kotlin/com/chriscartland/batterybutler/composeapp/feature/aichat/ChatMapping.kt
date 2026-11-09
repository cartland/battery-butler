package com.chriscartland.batterybutler.composeapp.feature.aichat

import com.chriscartland.batterybutler.domain.model.ai.AiCard
import com.chriscartland.batterybutler.domain.model.ai.AiMessage
import com.chriscartland.batterybutler.domain.model.ai.AiRole
import com.chriscartland.batterybutler.presentationfeature.aichat.ChatCard
import com.chriscartland.batterybutler.presentationfeature.aichat.ChatCardItem
import com.chriscartland.batterybutler.presentationfeature.aichat.ChatCardStatus
import com.chriscartland.batterybutler.presentationfeature.aichat.ChatChangeKind
import com.chriscartland.batterybutler.presentationfeature.aichat.ChatItemOutcome
import com.chriscartland.batterybutler.presentationfeature.aichat.ChatMessage

/** Domain chat entries as the UI shows them. Lives here because the UI layer can't see `domain.model.ai`. */
fun AiMessage.toChatMessage(cards: Map<String, AiCard>): ChatMessage =
    ChatMessage(
        id = id,
        text = text,
        isUser = role == AiRole.USER,
        isError = isError,
        card = cardId?.let(cards::get)?.toChatCard(),
        lookups = lookups,
    )

private fun AiCard.toChatCard(): ChatCard =
    ChatCard(
        id = id,
        items = items.map { item ->
            ChatCardItem(
                id = item.id,
                kind = ChatChangeKind.valueOf(item.kind.name),
                summary = item.summary,
                warning = item.warning,
                outcome = ChatItemOutcome.valueOf(item.outcome.name),
                reason = item.reason,
            )
        },
        status = ChatCardStatus.valueOf(status.name),
        canUndo = canUndo,
    )
