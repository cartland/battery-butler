package com.chriscartland.batterybutler.composeapp.feature.aichat

import com.chriscartland.batterybutler.domain.model.ai.AiAction
import com.chriscartland.batterybutler.domain.model.ai.AiActionKind
import com.chriscartland.batterybutler.domain.model.ai.AiMessage
import com.chriscartland.batterybutler.domain.model.ai.AiPendingAction
import com.chriscartland.batterybutler.domain.model.ai.AiRole
import com.chriscartland.batterybutler.presentationfeature.aichat.ChatAction
import com.chriscartland.batterybutler.presentationfeature.aichat.ChatActionKind
import com.chriscartland.batterybutler.presentationfeature.aichat.ChatMessage
import com.chriscartland.batterybutler.presentationfeature.aichat.ChatPendingAction

/** Domain chat entries as the UI shows them. Lives here because the UI layer can't see `domain.model.ai`. */
fun AiMessage.toChatMessage(): ChatMessage =
    ChatMessage(
        id = id,
        text = text,
        isUser = role == AiRole.USER,
        isError = isError,
        actions = actions.map { it.toChatAction() },
    )

fun AiAction.toChatAction(): ChatAction =
    ChatAction(
        id = id,
        kind = when (kind) {
            AiActionKind.ADDED -> ChatActionKind.ADDED
            AiActionKind.UPDATED -> ChatActionKind.UPDATED
            AiActionKind.RECORDED -> ChatActionKind.RECORDED
            AiActionKind.DELETED -> ChatActionKind.DELETED
            AiActionKind.MARKED -> ChatActionKind.MARKED
            AiActionKind.LOOKED_UP -> ChatActionKind.LOOKED_UP
        },
        summary = summary,
        undoable = undoable,
        undone = undone,
    )

fun AiPendingAction.toChatPendingAction(): ChatPendingAction = ChatPendingAction(id = id, summary = summary, consequence = consequence)
