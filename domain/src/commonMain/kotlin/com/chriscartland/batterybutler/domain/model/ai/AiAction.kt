package com.chriscartland.batterybutler.domain.model.ai

/**
 * Something the app did because the assistant asked it to, in the person's words.
 *
 * The summary is written by the code that made the change, from the real data, so it says what
 * happened rather than what the model believes happened. [undoable] is true while the app still
 * holds the way back; [undone] once the person has taken it.
 */
data class AiAction(
    val id: String,
    val kind: AiActionKind,
    val summary: String,
    val undoable: Boolean,
    val undone: Boolean = false,
)

enum class AiActionKind {
    ADDED,
    UPDATED,
    RECORDED,
    DELETED,
    MARKED,
    LOOKED_UP,
}

/**
 * A change the assistant asked for that the app will not make until the person says so.
 *
 * Only deletions get here. Everything else the assistant does is undoable, so it runs at once
 * and shows up as an [AiAction] with an undo instead. A deletion that takes a photo with it has
 * no way back, so it waits on a card. [consequence] names what would be lost.
 */
data class AiPendingAction(
    val id: String,
    val summary: String,
    val consequence: String? = null,
)
