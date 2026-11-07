package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.ai.AiAction

/**
 * What a tool did, for three readers: [modelResult] goes back to the model, [action] is the
 * record the person sees, and [undo] is the way back, held by the chat session until it's used
 * or the chat is cleared. A read or a refusal has no action and no undo.
 */
class ToolOutcome(
    val modelResult: String,
    val action: AiAction? = null,
    val undo: (suspend () -> Result<Unit, DataError>)? = null,
) {
    companion object {
        fun error(message: String) = ToolOutcome("Error: $message")
    }
}

/** What a deletion would do, shown on the confirmation card before anything happens. */
class DeletionPreview(
    val summary: String,
    val consequence: String?,
)
