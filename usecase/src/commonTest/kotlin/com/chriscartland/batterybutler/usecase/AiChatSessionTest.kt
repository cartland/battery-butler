package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.ai.AiCard
import com.chriscartland.batterybutler.domain.model.ai.AiCardStatus
import com.chriscartland.batterybutler.domain.model.ai.AiChangeKind
import com.chriscartland.batterybutler.domain.model.ai.AiItemOutcome
import com.chriscartland.batterybutler.domain.model.ai.AiMessage
import com.chriscartland.batterybutler.domain.model.ai.AiToolNames
import com.chriscartland.batterybutler.domain.model.ai.AiToolParams
import com.chriscartland.batterybutler.testcommon.FakeAiEngine
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.FakeNeedsBatteryRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AiChatSessionTest {
    private class Harness {
        val repo = FakeDeviceRepository().apply {
            setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Hall Alarm", location = "Hall")))
        }
        val marks = FakeNeedsBatteryRepository()
        val engine = FakeAiEngine()
        val session = AiChatSession(
            aiEngine = engine,
            writer = DeviceToolHandler(repo, marks, UpdateDeviceLastReplacedUseCase(repo)),
            reader = DeviceReadTools(repo, marks),
            buildAiContextUseCase = BuildAiContextUseCase(repo, marks),
        )

        fun script(vararg calls: Pair<String, Map<String, Any?>>) {
            engine.scriptedToolCalls = calls.toList()
        }

        suspend fun ask(text: String = "go"): AiMessage = session.send(text).toList().single()

        fun card(message: AiMessage): AiCard = assertNotNull(session.cards.value[assertNotNull(message.cardId)])

        fun card(id: String): AiCard = assertNotNull(session.cards.value[id])

        /** Ask for [calls] and return the card they produced, with the script cleared. */
        suspend fun proposed(vararg calls: Pair<String, Map<String, Any?>>): AiCard {
            script(*calls)
            val message = ask()
            script()
            return card(message)
        }
    }

    private val addPorch = AiToolNames.ADD_DEVICE to mapOf<String, Any?>(AiToolParams.NAME to "Porch Light")
    private val deleteHall = AiToolNames.DELETE_DEVICE to mapOf<String, Any?>(AiToolParams.ID to "d1")
    private val moveHall = AiToolNames.UPDATE_DEVICE to mapOf<String, Any?>(AiToolParams.ID to "d1", AiToolParams.NEW_LOCATION to "Landing")

    // ─── reads run, writes propose ──────────────────────────────────────────────

    @Test
    fun `a read-only tool runs and is noted without a card`() =
        runTest {
            val h = Harness()
            h.script(AiToolNames.GET_DEVICE_HISTORY to mapOf(AiToolParams.ID to "d1"))
            val reply = h.ask()
            assertNull(reply.cardId)
            assertEquals(listOf("Looked up the history of Hall Alarm"), reply.lookups)
            assertTrue(
                h.engine.recordedToolResults
                    .single()
                    .startsWith("History of 'Hall Alarm'"),
            )
        }

    @Test
    fun `every tool with a side effect proposes and changes nothing`() =
        runTest {
            val h = Harness()
            val writes = listOf(
                addPorch,
                AiToolNames.ADD_DEVICE_TYPE to mapOf(AiToolParams.NAME to "Lock", AiToolParams.BATTERY_TYPE to "AA", AiToolParams.BATTERY_QUANTITY to 4),
                AiToolNames.RECORD_BATTERY_REPLACEMENT to mapOf(AiToolParams.DEVICE_NAME to "Hall Alarm"),
                moveHall,
                AiToolNames.MARK_NEEDS_BATTERY to mapOf(AiToolParams.ID to "d1"),
                deleteHall,
            )
            val before = h.repo.devices.toList()
            val card = h.proposed(*writes.toTypedArray())
            assertEquals(writes.size, card.items.size)
            assertEquals(AiCardStatus.PENDING, card.status)
            assertEquals(before, h.repo.devices)
            assertTrue(h.repo.events.isEmpty())
            assertTrue(h.repo.deviceTypes.isEmpty())
            assertTrue(h.engine.recordedToolResults.all { it.contains("NOT done yet") }, h.engine.recordedToolResults.toString())
        }

    @Test
    fun `a tool nobody classified can't run`() =
        runTest {
            val h = Harness()
            h.script("eraseEverything" to emptyMap())
            assertNull(h.ask().cardId)
            assertTrue(
                h.engine.recordedToolResults
                    .single()
                    .startsWith("Error: Unknown tool"),
            )
        }

    @Test
    fun `steps show what the assistant has done so far and clear when it answers`() =
        runTest {
            val h = Harness()
            var midTurn = emptyList<String>()
            h.engine.afterToolCalls = { midTurn = h.session.steps.value }
            h.script(AiToolNames.GET_DEVICE_HISTORY to mapOf(AiToolParams.ID to "d1"), addPorch)
            h.ask()
            assertEquals(listOf("Looked up the history of Hall Alarm", "Drafted: Add Porch Light"), midTurn)
            assertTrue(
                h.session.steps.value
                    .isEmpty(),
            )
        }

    @Test
    fun `steps clear when a turn fails`() =
        runTest {
            val h = Harness()
            h.script(addPorch)
            h.engine.errorAfterToolCalls = IllegalStateException("boom")
            h.ask()
            assertTrue(
                h.session.steps.value
                    .isEmpty(),
            )
        }

    // ─── a bad model ────────────────────────────────────────────────────────────

    @Test
    fun `a model that says it's done is not shown over its own card`() =
        runTest {
            val h = Harness()
            h.engine.defaultResponseText = "Done! I've added the porch light."
            h.script(addPorch)
            val reply = h.ask()
            assertEquals("", reply.text)
            assertEquals(
                "Add Porch Light",
                h
                    .card(reply)
                    .items
                    .single()
                    .summary,
            )
        }

    @Test
    fun `a claimed change with no card gets a line saying nothing changed`() =
        runTest {
            val h = Harness()
            h.engine.defaultResponseText = "I've recorded that for you."
            val reply = h.ask()
            assertTrue(reply.text.startsWith("I've recorded that for you."))
            assertTrue(reply.text.contains("No changes were made"), reply.text)
        }

    @Test
    fun `an ordinary answer is left alone`() =
        runTest {
            val h = Harness()
            h.engine.defaultResponseText = "Your hall alarm takes a 9V."
            assertEquals("Your hall alarm takes a 9V.", h.ask().text)
        }

    @Test
    fun `tool syntax in a reply is removed`() =
        runTest {
            val h = Harness()
            h.engine.defaultResponseText = "Here you go.\nprint(default_api.addDevice(name=\"X\"))\n```json\n{\"tool\":\"addDevice\"}\n```"
            assertEquals("Here you go.", h.ask().text)
        }

    @Test
    fun `a reply that is only tool syntax becomes an honest error`() =
        runTest {
            val h = Harness()
            h.engine.defaultResponseText = "{\"tool\": \"addDevice\", \"args\": {}}"
            val reply = h.ask()
            assertTrue(reply.isError)
            assertTrue(reply.text.contains("Nothing in your data was changed"), reply.text)
        }

    @Test
    fun `an invented id makes no card and tells the model`() =
        runTest {
            val h = Harness()
            h.script(AiToolNames.DELETE_DEVICE to mapOf(AiToolParams.ID to "made-up"))
            assertNull(h.ask().cardId)
            assertTrue(
                h.engine.recordedToolResults
                    .single()
                    .startsWith("Error: Device not found"),
            )
        }

    @Test
    fun `a change that changes nothing makes no card`() =
        runTest {
            val h = Harness()
            h.script(AiToolNames.UPDATE_DEVICE to mapOf(AiToolParams.ID to "d1", AiToolParams.NEW_NAME to "Hall Alarm"))
            assertNull(h.ask().cardId)
        }

    @Test
    fun `the same proposal twice appears once`() =
        runTest {
            val h = Harness()
            val card = h.proposed(addPorch, addPorch, addPorch)
            assertEquals(1, card.items.size)
            assertTrue(
                h.engine.recordedToolResults
                    .last()
                    .contains("already on the card"),
            )
        }

    @Test
    fun `a model that won't stop reading runs out of budget`() =
        runTest {
            val h = Harness()
            h.script(*Array(20) { AiToolNames.LIST_DEVICES to mapOf<String, Any?>() })
            h.ask()
            assertTrue(
                h.engine.recordedToolResults
                    .last()
                    .startsWith("Error: read limit"),
            )
        }

    @Test
    fun `a model that won't stop proposing is capped`() =
        runTest {
            val h = Harness()
            val card = h.proposed(*Array(60) { AiToolNames.ADD_DEVICE to mapOf<String, Any?>(AiToolParams.NAME to "Device $it") })
            assertEquals(50, card.items.size)
            assertTrue(
                h.engine.recordedToolResults
                    .last()
                    .startsWith("Error: this message already proposes"),
            )
        }

    @Test
    fun `a turn that fails after proposing still shows the card and changed nothing`() =
        runTest {
            val h = Harness()
            h.script(addPorch)
            h.engine.errorAfterToolCalls = IllegalStateException("boom")
            val reply = h.ask()
            assertTrue(reply.isError)
            assertEquals(1, h.card(reply).items.size)
            assertEquals(1, h.repo.devices.size)
        }

    // ─── deciding ───────────────────────────────────────────────────────────────

    @Test
    fun `approving applies the change and offers undo on the same card`() =
        runTest {
            val h = Harness()
            val card = h.proposed(addPorch)
            h.session.approve(card.id)
            val after = h.card(card.id)
            assertEquals(AiCardStatus.APPLIED, after.status)
            assertEquals(AiItemOutcome.APPLIED, after.items.single().outcome)
            assertTrue(after.canUndo)
            assertEquals(2, h.repo.devices.size)
            h.ask()
            assertTrue(
                h.engine.recordedPrompts
                    .last()
                    .startsWith("[The user approved, and the app did this: Add Porch Light.]"),
                h.engine.recordedPrompts.last(),
            )
        }

    @Test
    fun `declining changes nothing and tells the model`() =
        runTest {
            val h = Harness()
            val card = h.proposed(deleteHall)
            h.session.decline(card.id)
            assertEquals(AiCardStatus.DECLINED, h.card(card.id).status)
            assertEquals(1, h.repo.devices.size)
            h.ask()
            assertTrue(
                h.engine.recordedPrompts
                    .last()
                    .startsWith("[The user declined this, so it was not done: Delete Hall Alarm.]"),
            )
        }

    @Test
    fun `a new message withdraws a waiting card`() =
        runTest {
            val h = Harness()
            val card = h.proposed(deleteHall)
            h.ask("actually, what's due?")
            assertEquals(AiCardStatus.WITHDRAWN, h.card(card.id).status)
            h.session.approve(card.id)
            assertEquals(1, h.repo.devices.size, "a withdrawn card can't be applied")
            assertTrue(
                h.engine.recordedPrompts
                    .last()
                    .contains("instead of answering"),
            )
        }

    @Test
    fun `items can be left out of a card`() =
        runTest {
            val h = Harness()
            val card = h.proposed(addPorch, AiToolNames.ADD_DEVICE to mapOf(AiToolParams.NAME to "Gate Sensor"))
            h.session.approve(card.id, leftOut = setOf(card.items[1].id))
            assertEquals(listOf("Hall Alarm", "Porch Light"), h.repo.devices.map { it.name })
            assertEquals(AiItemOutcome.LEFT_OUT, h.card(card.id).items[1].outcome)
            assertEquals(AiCardStatus.APPLIED, h.card(card.id).status)
        }

    @Test
    fun `approving twice applies once`() =
        runTest {
            val h = Harness()
            val card = h.proposed(AiToolNames.RECORD_BATTERY_REPLACEMENT to mapOf(AiToolParams.DEVICE_NAME to "Hall Alarm"))
            h.session.approve(card.id)
            h.session.approve(card.id)
            assertEquals(1, h.repo.events.size)
        }

    @Test
    fun `a row that changed after the proposal is left alone and says why`() =
        runTest {
            val h = Harness()
            val card = h.proposed(moveHall)
            h.repo.setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Renamed on the web", location = "Hall")))
            h.session.approve(card.id)
            val after = h.card(card.id)
            assertEquals(AiCardStatus.FAILED, after.status)
            assertEquals(AiItemOutcome.CHANGED_SINCE, after.items.single().outcome)
            assertTrue(
                after.items
                    .single()
                    .reason!!
                    .contains("changed after"),
            )
            assertEquals(
                "Hall",
                h.repo.devices
                    .single()
                    .location,
            )
            assertFalse(after.canUndo)
        }

    @Test
    fun `a background sync rewriting a row the same way doesn't make the card stale`() =
        runTest {
            val h = Harness()
            h.repo.setEvents(listOf(TestDevices.createBatteryEvent(id = "e1", deviceId = "d1", batteryType = "9V", notes = "old")))
            val card = h.proposed(AiToolNames.UPDATE_BATTERY_EVENT to mapOf(AiToolParams.ID to "e1", AiToolParams.NOTES to "fresh pack"))
            // What a snapshot does: the wire has no battery type, so the row comes back without one.
            h.repo.setEvents(listOf(TestDevices.createBatteryEvent(id = "e1", deviceId = "d1", batteryType = null, notes = "old")))
            h.repo.setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Hall Alarm", location = "Hall", lastUpdated = kotlin.time.Instant.fromEpochMilliseconds(99))))
            h.session.approve(card.id)
            assertEquals(
                AiCardStatus.APPLIED,
                h.card(card.id).status,
                h
                    .card(card.id)
                    .items
                    .single()
                    .reason,
            )
            assertEquals(
                "fresh pack",
                h.repo.events
                    .single()
                    .notes,
            )
        }

    @Test
    fun `a change that's already been made elsewhere says so`() =
        runTest {
            val h = Harness()
            val card = h.proposed(moveHall)
            h.repo.setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Hall Alarm", location = "Landing")))
            h.session.approve(card.id)
            val item = h.card(card.id).items.single()
            assertEquals(AiItemOutcome.CHANGED_SINCE, item.outcome)
            assertTrue(item.reason!!.contains("Nothing to change"), item.reason)
        }

    @Test
    fun `two changes to one device on one card don't conflict with each other`() =
        runTest {
            val h = Harness()
            val card = h.proposed(moveHall, AiToolNames.UPDATE_DEVICE to mapOf(AiToolParams.ID to "d1", AiToolParams.NEW_NAME to "Landing Alarm"))
            h.session.approve(card.id)
            assertEquals(AiCardStatus.APPLIED, h.card(card.id).status)
            assertEquals(
                "Landing Alarm",
                h.repo.devices
                    .single()
                    .name,
            )
            assertEquals(
                "Landing",
                h.repo.devices
                    .single()
                    .location,
            )
        }

    @Test
    fun `one failure doesn't stop the rest and the card says which`() =
        runTest {
            val h = Harness()
            val card = h.proposed(addPorch, AiToolNames.ADD_DEVICE to mapOf(AiToolParams.NAME to "Gate Sensor"))
            h.repo.setDevices(h.repo.devices + TestDevices.createDevice(id = "d2", name = "Porch Light"))
            h.session.approve(card.id)
            val after = h.card(card.id)
            assertEquals(AiCardStatus.PARTLY_APPLIED, after.status)
            assertEquals(AiItemOutcome.CHANGED_SINCE, after.items[0].outcome)
            assertTrue(after.items[0].reason!!.contains("already exists"))
            assertEquals(AiItemOutcome.APPLIED, after.items[1].outcome)
        }

    // ─── undo ───────────────────────────────────────────────────────────────────

    @Test
    fun `undo reverses the card and tells the model`() =
        runTest {
            val h = Harness()
            val card = h.proposed(addPorch)
            h.session.approve(card.id)
            h.session.undo(card.id)
            val after = h.card(card.id)
            assertEquals(AiCardStatus.UNDONE, after.status)
            assertEquals(AiItemOutcome.UNDONE, after.items.single().outcome)
            assertFalse(after.canUndo)
            assertEquals(1, h.repo.devices.size)
            h.ask()
            assertTrue(
                h.engine.recordedPrompts
                    .last()
                    .contains("[The user undid this: Add Porch Light. Treat it as not having happened.]"),
            )
        }

    @Test
    fun `undoing twice is one undo`() =
        runTest {
            val h = Harness()
            val card = h.proposed(AiToolNames.RECORD_BATTERY_REPLACEMENT to mapOf(AiToolParams.DEVICE_NAME to "Hall Alarm"))
            h.session.approve(card.id)
            h.session.undo(card.id)
            h.session.undo(card.id)
            assertEquals(AiCardStatus.UNDONE, h.card(card.id).status)
            assertTrue(h.repo.events.isEmpty())
        }

    @Test
    fun `undo runs newest first so a batch comes apart cleanly`() =
        runTest {
            val h = Harness()
            val card = h.proposed(addPorch, AiToolNames.RECORD_BATTERY_REPLACEMENT to mapOf(AiToolParams.DEVICE_NAME to "Porch Light"))
            h.session.approve(card.id)
            assertEquals(1, h.repo.events.size)
            h.session.undo(card.id)
            assertEquals(AiCardStatus.UNDONE, h.card(card.id).status)
            assertEquals(listOf("Hall Alarm"), h.repo.devices.map { it.name })
            assertTrue(h.repo.events.isEmpty())
        }

    @Test
    fun `an undo that can't happen says why on the card and can be tried again`() =
        runTest {
            val h = Harness()
            val card = h.proposed(addPorch)
            h.session.approve(card.id)
            val later = h.proposed(AiToolNames.RECORD_BATTERY_REPLACEMENT to mapOf(AiToolParams.DEVICE_NAME to "Porch Light"))
            h.session.approve(later.id)

            h.session.undo(card.id)
            val stuck = h.card(card.id)
            assertEquals(AiCardStatus.PARTLY_UNDONE, stuck.status)
            assertEquals(AiItemOutcome.UNDO_FAILED, stuck.items.single().outcome)
            assertTrue(
                stuck.items
                    .single()
                    .reason!!
                    .contains("battery changes recorded since"),
                stuck.items.single().reason,
            )
            assertTrue(stuck.canUndo, "the way back isn't lost")

            h.session.undo(later.id)
            h.session.undo(card.id)
            assertEquals(AiCardStatus.UNDONE, h.card(card.id).status)
            assertEquals(1, h.repo.devices.size)
        }

    @Test
    fun `an outcome note survives a turn that fails`() =
        runTest {
            val h = Harness()
            val card = h.proposed(addPorch)
            h.session.approve(card.id)
            h.session.undo(card.id)

            h.engine.errorReplyText = "The assistant is temporarily unavailable."
            assertTrue(h.ask().isError)
            h.engine.errorReplyText = null

            h.ask("hello again")
            assertTrue(
                h.engine.recordedPrompts
                    .last()
                    .contains("The user undid this: Add Porch Light."),
                h.engine.recordedPrompts.last(),
            )
        }

    @Test
    fun `reset forgets the conversation and the cards`() =
        runTest {
            val h = Harness()
            val card = h.proposed(addPorch)
            h.session.reset()
            assertEquals(1, h.engine.resetCount)
            assertTrue(
                h.session.cards.value
                    .isEmpty(),
            )
            h.session.approve(card.id)
            assertEquals(1, h.repo.devices.size)
            h.ask("hi")
            assertEquals("hi", h.engine.recordedPrompts.last())
        }

    @Test
    fun `the kind of each change is on the card`() =
        runTest {
            val h = Harness()
            val card = h.proposed(deleteHall)
            assertEquals(AiChangeKind.DELETE, card.items.single().kind)
        }
}
