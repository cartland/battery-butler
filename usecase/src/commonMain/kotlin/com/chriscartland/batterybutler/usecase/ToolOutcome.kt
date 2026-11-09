package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.ai.AiChangeKind

/** What applying a change did: a result for the model and, when it wrote, the way back. */
class ToolOutcome(
    val modelResult: String,
    val undo: (suspend () -> Result<Unit, DataError>)? = null,
    val failed: Boolean = false,
) {
    companion object {
        fun error(message: String) = ToolOutcome("Error: $message", failed = true)
    }
}

/** What a change would do, worked out from the real data without writing anything. */
sealed interface ToolPreview {
    /** [target] and [basis] name the row it depends on, to notice if that row changes first. */
    class Ready(
        val kind: AiChangeKind,
        val summary: String,
        val warning: String? = null,
        val target: String? = null,
        val basis: String? = null,
    ) : ToolPreview

    /** Nothing to put on a card: bad arguments, an unknown id, or no actual change. */
    class Refused(
        val message: String,
    ) : ToolPreview
}

/** What a read-only tool found, and how to say so in the transcript. */
class ReadResult(
    val modelResult: String,
    val lookup: String? = null,
)
