package com.chriscartland.batterybutler.presentationfeature.aichat

/** UI model for one entry in the chat transcript. */
data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false,
    val card: ChatCard? = null,
    val lookups: List<String> = emptyList(),
)

/** Changes the assistant proposed, with their outcome once the person has decided. */
data class ChatCard(
    val id: String,
    val items: List<ChatCardItem>,
    val status: ChatCardStatus,
    val canUndo: Boolean = false,
)

data class ChatCardItem(
    val id: String,
    val kind: ChatChangeKind,
    val summary: String,
    val warning: String? = null,
    val outcome: ChatItemOutcome = ChatItemOutcome.PROPOSED,
    val reason: String? = null,
)

enum class ChatChangeKind {
    ADD,
    UPDATE,
    RECORD,
    DELETE,
    MARK,
}

enum class ChatItemOutcome {
    PROPOSED,
    LEFT_OUT,
    APPLIED,
    FAILED,
    CHANGED_SINCE,
    UNDONE,
    UNDO_FAILED,
}

enum class ChatCardStatus {
    PENDING,
    APPLYING,
    APPLIED,
    PARTLY_APPLIED,
    FAILED,
    DECLINED,
    WITHDRAWN,
    UNDOING,
    UNDONE,
    PARTLY_UNDONE,
}
