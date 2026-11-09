package com.chriscartland.batterybutler.domain.model.ai

/**
 * A change the assistant wants to make, shown to the person before anything happens.
 *
 * The app writes every word on it from the real data, so it says what would actually change
 * however confused the model is. The same card then carries the outcome and the Undo.
 */
data class AiCard(
    val id: String,
    val items: List<AiCardItem>,
    val status: AiCardStatus,
    val canUndo: Boolean = false,
)

/** One change on a card. [reason] says why, in words, whenever it didn't go as proposed. */
data class AiCardItem(
    val id: String,
    val kind: AiChangeKind,
    val summary: String,
    val warning: String? = null,
    val outcome: AiItemOutcome = AiItemOutcome.PROPOSED,
    val reason: String? = null,
)

enum class AiChangeKind {
    ADD,
    UPDATE,
    RECORD,
    DELETE,
    MARK,
}

enum class AiItemOutcome {
    PROPOSED,
    LEFT_OUT,
    APPLIED,
    FAILED,
    CHANGED_SINCE,
    UNDONE,
    UNDO_FAILED,
}

enum class AiCardStatus {
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
