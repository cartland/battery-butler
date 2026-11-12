package com.chriscartland.batterybutler.usecase

import co.touchlab.kermit.Logger
import com.benasher44.uuid.uuid4
import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.ai.AiCard
import com.chriscartland.batterybutler.domain.model.ai.AiCardItem
import com.chriscartland.batterybutler.domain.model.ai.AiCardStatus
import com.chriscartland.batterybutler.domain.model.ai.AiEngine
import com.chriscartland.batterybutler.domain.model.ai.AiItemOutcome
import com.chriscartland.batterybutler.domain.model.ai.AiMessage
import com.chriscartland.batterybutler.domain.model.ai.AiRole
import com.chriscartland.batterybutler.domain.model.ai.AiToolNames
import com.chriscartland.batterybutler.domain.model.ai.ToolHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.tatarka.inject.annotations.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * One conversation with the assistant. The model can read, and it can ask. Everything else is here,
 * so the guarantees hold however badly the model behaves:
 *
 * - **Reads run; writes propose.** A tool in [AiToolNames.READ_ONLY] goes to [DeviceReadTools]. Any
 *   other call is previewed and put on a card, and the model is told nothing has happened yet.
 * - **The card is the truth.** Its words come from the real data. A reply that proposes something
 *   shows only the card, so a model that says "done" can't contradict it.
 * - **Apply and Undo happen once.** Each is gated on the card's status, so a double tap is a no-op.
 *   Every item reports its own outcome, and whatever goes wrong is written on the card with the
 *   reason.
 * - **The model hears what it can't see.** Approvals, declines and undos go at the top of the next
 *   message, and survive a failed turn.
 */
@Inject
class AiChatSession(
    private val aiEngine: AiEngine,
    private val writer: DeviceToolHandler,
    private val reader: DeviceReadTools,
    private val buildAiContextUseCase: BuildAiContextUseCase,
) {
    private val _cards = MutableStateFlow<Map<String, AiCard>>(emptyMap())
    val cards: StateFlow<Map<String, AiCard>> = _cards.asStateFlow()

    private val mutex = Mutex()
    private val proposals = mutableMapOf<String, List<Proposal>>()
    private val undos = mutableMapOf<String, suspend () -> Result<Unit, DataError>>()
    private val notes = mutableListOf<String>()

    private class Proposal(
        val itemId: String,
        val name: String,
        val args: Map<String, Any?>,
        val item: AiCardItem,
    )

    /**
     * Send one message. Nothing the model does here can change data, so a turn that fails partway
     * has changed nothing. [hints] are facts about where the person is, shown to the model only.
     */
    fun send(
        text: String,
        hints: Map<String, String> = emptyMap(),
    ): Flow<AiMessage> =
        flow {
            withdrawPending()
            val sentNotes = notes.toList()
            notes.clear()
            val prompt = buildString {
                sentNotes.forEach { appendLine("[$it]") }
                hints.forEach { (k, v) -> appendLine("[$k: $v]") }
                if (isNotEmpty()) appendLine()
                append(text)
            }

            val turn = Turn()
            val tools = ToolHandler { name, args ->
                if (name in AiToolNames.READ_ONLY) read(name, args, turn) else propose(name, args, turn)
            }
            try {
                aiEngine.generateResponse(prompt, tools) { buildAiContextUseCase() }.collect { message ->
                    emit(if (message.isPartial) message else finish(message, turn, sentNotes))
                }
                if (turn.draft.isNotEmpty()) emit(finish(AiMessage(id = "card_${uuid4()}", role = AiRole.MODEL, text = ""), turn, sentNotes))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(TAG, e) { "chat turn failed" }
                val failure = AiMessage(id = "error_${uuid4()}", role = AiRole.MODEL, text = FAILED, isError = true)
                emit(finish(failure, turn, sentNotes))
            }
        }

    private class Turn {
        val draft = mutableListOf<Proposal>()
        val lookups = mutableListOf<String>()
        var reads = 0
    }

    private suspend fun read(
        name: String,
        args: Map<String, Any?>,
        turn: Turn,
    ): String {
        if (++turn.reads > MAX_READS) return "Error: read limit reached for this message. Answer with what you have."
        val result = reader.run(name, args)
        result.lookup?.let { if (it !in turn.lookups) turn.lookups += it }
        return result.modelResult
    }

    private suspend fun propose(
        name: String,
        args: Map<String, Any?>,
        turn: Turn,
    ): String {
        if (turn.draft.size >= MAX_ITEMS) return "Error: this message already proposes $MAX_ITEMS changes. Stop, and propose the rest after these."
        if (turn.draft.any { it.name == name && it.args == args }) return ALREADY_PROPOSED
        return when (val preview = writer.preview(name, args)) {
            is ToolPreview.Refused -> {
                preview.message
            }

            is ToolPreview.Ready -> {
                val itemId = "item_${uuid4()}"
                turn.draft += Proposal(
                    itemId = itemId,
                    name = name,
                    args = args,
                    item = AiCardItem(id = itemId, kind = preview.kind, summary = preview.summary, warning = preview.warning),
                )
                "Proposed to the user on a card: \"${preview.summary}\". NOT done yet: they will approve or decline it. " +
                    "Propose any other changes this request needs, then stop."
            }
        }
    }

    /** The reply as shown: the card if it proposed anything, otherwise the model's words, checked. */
    private fun finish(
        message: AiMessage,
        turn: Turn,
        sentNotes: List<String>,
    ): AiMessage {
        val cardId = publish(turn)
        val lookups = turn.lookups.toList().also { turn.lookups.clear() }
        var failed = message.isError
        val text = when {
            message.isError -> {
                message.text
            }

            cardId != null -> {
                ""
            }

            else -> {
                val cleaned = AiReplyGuard.clean(message.text)
                when {
                    cleaned.isBlank() -> {
                        failed = true
                        UNUSABLE
                    }

                    AiReplyGuard.claimsAChange(cleaned) -> {
                        "$cleaned\n\n$NOTHING_CHANGED_LINE"
                    }

                    else -> {
                        cleaned
                    }
                }
            }
        }
        if (failed) notes.addAll(0, sentNotes)
        return message.copy(text = text, isError = failed, cardId = cardId, lookups = lookups)
    }

    private fun publish(turn: Turn): String? {
        if (turn.draft.isEmpty()) return null
        val cardId = "card_${uuid4()}"
        proposals[cardId] = turn.draft.toList()
        put(AiCard(id = cardId, items = turn.draft.map { it.item }, status = AiCardStatus.PENDING))
        turn.draft.clear()
        return cardId
    }

    /** The person approved, leaving out [leftOut]. Each change is applied and reported on its own. */
    suspend fun approve(
        cardId: String,
        leftOut: Set<String> = emptySet(),
    ) = mutex.withLock {
        val card = _cards.value[cardId]?.takeIf { it.status == AiCardStatus.PENDING } ?: return@withLock
        val planned = proposals[cardId].orEmpty()
        put(card.copy(status = AiCardStatus.APPLYING))

        // Check everything against the data as it is now, before applying anything: a change
        // earlier on this card will alter what a later one sees, and that isn't a conflict.
        val stale = planned.filter { it.itemId !in leftOut }.associate { it.itemId to recheck(it) }
        val results = planned.map { proposal ->
            val item = proposal.item
            val reason = stale[proposal.itemId]
            when {
                proposal.itemId in leftOut -> {
                    item.copy(outcome = AiItemOutcome.LEFT_OUT)
                }

                reason != null -> {
                    item.copy(outcome = AiItemOutcome.CHANGED_SINCE, reason = reason)
                }

                else -> {
                    val outcome = writer.run(proposal.name, proposal.args)
                    if (outcome.failed) {
                        Logger.w(TAG) { "apply failed: ${proposal.name}: ${outcome.modelResult}" }
                        item.copy(outcome = AiItemOutcome.FAILED, reason = outcome.modelResult.removePrefix("Error: "))
                    } else {
                        outcome.undo?.let { undos[proposal.itemId] = it }
                        item.copy(outcome = AiItemOutcome.APPLIED)
                    }
                }
            }
        }
        proposals.remove(cardId)

        val applied = results.filter { it.outcome == AiItemOutcome.APPLIED }
        val problems = results.filter { it.outcome == AiItemOutcome.FAILED || it.outcome == AiItemOutcome.CHANGED_SINCE }
        val status = when {
            applied.isEmpty() -> AiCardStatus.FAILED
            problems.isNotEmpty() -> AiCardStatus.PARTLY_APPLIED
            else -> AiCardStatus.APPLIED
        }
        put(card.copy(items = results, status = status, canUndo = results.any { it.id in undos }))
        if (applied.isNotEmpty()) notes += "The user approved, and the app did this: ${joined(applied)}."
        if (problems.isNotEmpty()) notes += "These were not done: ${problems.joinToString("; ") { "${it.summary} (${it.reason})" }}."
        results.filter { it.outcome == AiItemOutcome.LEFT_OUT }.takeIf { it.isNotEmpty() }?.let {
            notes += "The user left these out, so they were not done: ${joined(it)}."
        }
    }

    /**
     * Null if approving would still do exactly what the card says; otherwise why not. The card's
     * own words are the test, so only a difference the person could see counts. Comparing rows
     * instead refused changes that a background sync had merely rewritten in an equivalent form.
     */
    private suspend fun recheck(proposal: Proposal): String? =
        when (val now = writer.preview(proposal.name, proposal.args)) {
            is ToolPreview.Refused -> {
                now.message.removePrefix("Error: ").removePrefix("Success: ")
            }

            is ToolPreview.Ready -> {
                if (now.summary == proposal.item.summary && now.warning == proposal.item.warning) {
                    null
                } else {
                    Logger.w(TAG) { "changed since proposed: '${proposal.item.summary}' is now '${now.summary}'" }
                    "$CHANGED_SINCE Now it would be: ${now.summary}."
                }
            }
        }

    /** The person declined. Nothing happens, and the model is told. */
    suspend fun decline(cardId: String) =
        mutex.withLock {
            val card = _cards.value[cardId]?.takeIf { it.status == AiCardStatus.PENDING } ?: return@withLock
            proposals.remove(cardId)
            put(card.copy(status = AiCardStatus.DECLINED))
            notes += "The user declined this, so it was not done: ${joined(card.items)}."
        }

    /** Take a card back, newest change first. Whatever can't be undone says why on the card. */
    suspend fun undo(cardId: String) =
        mutex.withLock {
            val card = _cards.value[cardId]?.takeIf { it.canUndo && it.status in UNDOABLE } ?: return@withLock
            put(card.copy(status = AiCardStatus.UNDOING, canUndo = false))
            val updated = card.items.toMutableList()
            for (index in updated.indices.reversed()) {
                val item = updated[index]
                val step = undos[item.id] ?: continue
                updated[index] = when (val result = step()) {
                    is Result.Success -> {
                        undos.remove(item.id)
                        item.copy(outcome = AiItemOutcome.UNDONE, reason = null)
                    }

                    is Result.Error -> {
                        Logger.w(TAG) { "undo failed: ${item.summary}: ${result.error.message}" }
                        item.copy(outcome = AiItemOutcome.UNDO_FAILED, reason = result.error.message)
                    }
                }
            }
            val undone = updated.filter { it.outcome == AiItemOutcome.UNDONE }
            val stuck = updated.any { it.id in undos }
            put(card.copy(items = updated, status = if (stuck) AiCardStatus.PARTLY_UNDONE else AiCardStatus.UNDONE, canUndo = stuck))
            val newlyUndone = undone.filter { old -> card.items.first { it.id == old.id }.outcome != AiItemOutcome.UNDONE }
            if (newlyUndone.isNotEmpty()) notes += "The user undid this: ${joined(newlyUndone)}. Treat it as not having happened."
        }

    private fun withdrawPending() {
        _cards.value.values.filter { it.status == AiCardStatus.PENDING }.forEach { card ->
            proposals.remove(card.id)
            put(card.copy(status = AiCardStatus.WITHDRAWN))
            notes += "The user sent a new message instead of answering, so this was not done: ${joined(card.items)}."
        }
    }

    /** Forget everything: the conversation, the cards, and every way back. */
    suspend fun reset() {
        mutex.withLock {
            proposals.clear()
            undos.clear()
            notes.clear()
            _cards.value = emptyMap()
        }
        aiEngine.resetConversation()
    }

    private fun put(card: AiCard) {
        _cards.value = _cards.value + (card.id to card)
    }

    private fun joined(items: List<AiCardItem>) = items.joinToString("; ") { it.summary }

    private companion object {
        const val TAG = "AiChatSession"
        const val MAX_READS = 12
        const val MAX_ITEMS = 50
        const val ALREADY_PROPOSED = "That change is already on the card. Don't propose it again."
        const val CHANGED_SINCE = "This changed after it was proposed, so it was left alone."
        const val FAILED = "Something went wrong and the assistant couldn't answer. Nothing in your data was changed."
        const val UNUSABLE = "The assistant didn't give a usable answer. Nothing in your data was changed."
        const val NOTHING_CHANGED_LINE = "(No changes were made. Changes only happen from a card you approve.)"
        val UNDOABLE = setOf(AiCardStatus.APPLIED, AiCardStatus.PARTLY_APPLIED, AiCardStatus.PARTLY_UNDONE)
    }
}
