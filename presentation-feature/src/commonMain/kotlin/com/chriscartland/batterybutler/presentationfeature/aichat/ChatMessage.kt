package com.chriscartland.batterybutler.presentationfeature.aichat

/**
 * UI model for one entry in the chat transcript.
 *
 * A message with no text and some [actions] is an entry the app wrote itself: a confirmed
 * deletion, for instance. It renders as the action rows alone.
 */
data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false,
    val actions: List<ChatAction> = emptyList(),
)

/** One thing the app did for the assistant, as the app describes it. */
data class ChatAction(
    val id: String,
    val kind: ChatActionKind,
    val summary: String,
    val undoable: Boolean,
    val undone: Boolean = false,
)

enum class ChatActionKind {
    ADDED,
    UPDATED,
    RECORDED,
    DELETED,
    MARKED,
    LOOKED_UP,
}

/** A deletion the assistant asked for, waiting on the person. */
data class ChatPendingAction(
    val id: String,
    val summary: String,
    val consequence: String? = null,
)
