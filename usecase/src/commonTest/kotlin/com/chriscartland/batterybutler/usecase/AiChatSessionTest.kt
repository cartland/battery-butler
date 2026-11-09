package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.ai.AiActionKind
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
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AiChatSessionTest {
    private class Harness {
        val repo = FakeDeviceRepository()
        val engine = FakeAiEngine()
        val session = AiChatSession(
            aiEngine = engine,
            toolHandler = DeviceToolHandler(repo, FakeNeedsBatteryRepository(), UpdateDeviceLastReplacedUseCase(repo)),
            buildAiContextUseCase = BuildAiContextUseCase(repo, FakeNeedsBatteryRepository()),
        )

        init {
            repo.setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Hall Alarm")))
        }
    }

    @Test
    fun `the situation goes with the prompt and not inside it`() =
        runTest {
            val h = Harness()
            h.session.send("hello", hints = mapOf("Message sent from" to "Devices tab")).toList()
            assertEquals("[Message sent from: Devices tab]\n\nhello", h.engine.recordedPrompts.single())
            assertTrue(
                h.engine.recordedSituations
                    .single()
                    .contains("Hall Alarm"),
            )
        }

    @Test
    fun `a write runs at once and the reply carries its action`() =
        runTest {
            val h = Harness()
            h.engine.scriptedToolCalls = listOf(AiToolNames.ADD_DEVICE to mapOf(AiToolParams.NAME to "Porch Light"))
            val reply = h.session
                .send("add a porch light")
                .toList()
                .single()
            assertEquals(2, h.repo.devices.size)
            val action = reply.actions.single()
            assertEquals(AiActionKind.ADDED, action.kind)
            assertTrue(h.session.canUndo(action.id))
            assertTrue(
                h.engine.recordedToolResults
                    .single()
                    .startsWith("Success:"),
            )
        }

    @Test
    fun `a deletion waits for the person and the model is told to stop`() =
        runTest {
            val h = Harness()
            h.engine.scriptedToolCalls = listOf(AiToolNames.DELETE_DEVICE to mapOf(AiToolParams.ID to "d1"))
            val reply = h.session
                .send("delete the hall alarm")
                .toList()
                .single()

            assertEquals(1, h.repo.devices.size, "nothing is deleted on the model's say-so")
            assertTrue(reply.actions.isEmpty())
            val pending = assertNotNull(h.session.pendingAction.value)
            assertEquals("Delete Hall Alarm", pending.summary)
            val told = h.engine.recordedToolResults.single()
            assertTrue(told.startsWith("Waiting for the user"), told)
            assertTrue(told.contains("Do not call this tool again"))
        }

    @Test
    fun `confirming runs the deletion and keeps a way back and and tells the model next turn`() =
        runTest {
            val h = Harness()
            h.engine.scriptedToolCalls = listOf(AiToolNames.DELETE_DEVICE to mapOf(AiToolParams.ID to "d1"))
            h.session.send("delete it").toList()
            h.engine.scriptedToolCalls = emptyList()

            val outcome = assertNotNull(h.session.confirmPendingAction())
            assertTrue(h.repo.devices.isEmpty())
            assertNull(h.session.pendingAction.value)
            val action = assertNotNull(outcome.action)
            assertTrue(h.session.canUndo(action.id))

            h.session.send("thanks").toList()
            assertTrue(
                h.engine.recordedPrompts
                    .last()
                    .startsWith("[The user confirmed, and the app did this: Deleted Hall Alarm.]"),
            )
            assertNull(h.session.confirmPendingAction(), "nothing left to confirm")
        }

    @Test
    fun `declining deletes nothing and tells the model`() =
        runTest {
            val h = Harness()
            h.engine.scriptedToolCalls = listOf(AiToolNames.DELETE_DEVICE to mapOf(AiToolParams.ID to "d1"))
            h.session.send("delete it").toList()
            h.engine.scriptedToolCalls = emptyList()

            assertNotNull(h.session.cancelPendingAction())
            assertEquals(1, h.repo.devices.size)
            assertNull(h.session.pendingAction.value)
            h.session.send("ok").toList()
            assertTrue(
                h.engine.recordedPrompts
                    .last()
                    .startsWith("[The user declined \"Delete Hall Alarm\". Nothing was deleted.]"),
            )
        }

    @Test
    fun `a new message withdraws a waiting deletion`() =
        runTest {
            val h = Harness()
            h.engine.scriptedToolCalls = listOf(AiToolNames.DELETE_DEVICE to mapOf(AiToolParams.ID to "d1"))
            h.session.send("delete it").toList()
            h.engine.scriptedToolCalls = emptyList()

            h.session.send("actually, what's due?").toList()
            assertNull(h.session.pendingAction.value)
            assertEquals(1, h.repo.devices.size)
            assertTrue(
                h.engine.recordedPrompts
                    .last()
                    .contains("instead of confirming"),
            )
        }

    @Test
    fun `only one deletion can wait at a time`() =
        runTest {
            val h = Harness()
            h.repo.setDevices(listOf(TestDevices.createDevice(id = "d1", name = "A"), TestDevices.createDevice(id = "d2", name = "B")))
            h.engine.scriptedToolCalls = listOf(
                AiToolNames.DELETE_DEVICE to mapOf(AiToolParams.ID to "d1"),
                AiToolNames.DELETE_DEVICE to mapOf(AiToolParams.ID to "d2"),
            )
            h.session.send("delete both").toList()
            assertEquals(
                "Delete A",
                h.session.pendingAction.value
                    ?.summary,
            )
            assertTrue(h.engine.recordedToolResults[1].startsWith("Error: another deletion is already waiting"))
        }

    @Test
    fun `a deletion of something that doesn't exist is an ordinary error`() =
        runTest {
            val h = Harness()
            h.engine.scriptedToolCalls = listOf(AiToolNames.DELETE_DEVICE to mapOf(AiToolParams.ID to "ghost"))
            h.session.send("delete it").toList()
            assertNull(h.session.pendingAction.value)
            assertTrue(
                h.engine.recordedToolResults
                    .single()
                    .startsWith("Error: Device not found"),
            )
        }

    @Test
    fun `undo takes the change back once and tells the model`() =
        runTest {
            val h = Harness()
            h.engine.scriptedToolCalls = listOf(AiToolNames.ADD_DEVICE to mapOf(AiToolParams.NAME to "Porch Light"))
            val action = h.session
                .send("add it")
                .toList()
                .single()
                .actions
                .single()
            h.engine.scriptedToolCalls = emptyList()

            assertIs<Result.Success<Unit>>(h.session.undo(action.id))
            assertEquals(1, h.repo.devices.size)
            assertIs<Result.Error<*>>(h.session.undo(action.id), "a second undo has nothing to take back")

            h.session.send("ok").toList()
            assertTrue(
                h.engine.recordedPrompts
                    .last()
                    .startsWith("[The user undid this: Added Porch Light. Treat it as not having happened.]"),
            )
        }

    @Test
    fun `a turn that fails after a write keeps the action and tells the model next time`() =
        runTest {
            val h = Harness()
            h.engine.scriptedToolCalls = listOf(AiToolNames.ADD_DEVICE to mapOf(AiToolParams.NAME to "Porch Light"))
            h.engine.errorAfterToolCalls = IllegalStateException("boom")
            val reply = h.session
                .send("add it")
                .toList()
                .single()

            assertTrue(reply.isError)
            assertEquals(1, reply.actions.size)
            assertTrue(reply.text.contains("changes listed here were made"), reply.text)
            assertTrue(h.session.canUndo(reply.actions.single().id))

            h.engine.errorAfterToolCalls = null
            h.engine.scriptedToolCalls = emptyList()
            h.session.send("did that work?").toList()
            assertTrue(
                h.engine.recordedPrompts
                    .last()
                    .contains("already done this: Added Porch Light"),
                h.engine.recordedPrompts.last(),
            )
        }

    @Test
    fun `a turn that fails before any write says nothing changed`() =
        runTest {
            val h = Harness()
            h.engine.errorAfterToolCalls = IllegalStateException("boom")
            val reply = h.session
                .send("hello")
                .toList()
                .single()
            assertTrue(reply.isError)
            assertTrue(reply.text.contains("Nothing in your data was changed"), reply.text)
        }

    @Test
    fun `an outcome note survives a turn that fails`() =
        runTest {
            val h = Harness()
            h.engine.scriptedToolCalls = listOf(AiToolNames.ADD_DEVICE to mapOf(AiToolParams.NAME to "Porch Light"))
            val action = h.session
                .send("add it")
                .toList()
                .single()
                .actions
                .single()
            h.engine.scriptedToolCalls = emptyList()
            h.session.undo(action.id)

            h.engine.errorReplyText = "The assistant is temporarily unavailable."
            assertTrue(
                h.session
                    .send("hello")
                    .toList()
                    .single()
                    .isError,
            )
            h.engine.errorReplyText = null

            h.session.send("hello again").toList()
            assertTrue(
                h.engine.recordedPrompts
                    .last()
                    .startsWith("[The user undid this: Added Porch Light."),
                h.engine.recordedPrompts.last(),
            )
        }

    @Test
    fun `reset forgets the conversation and the way back and and the waiting deletion`() =
        runTest {
            val h = Harness()
            h.engine.scriptedToolCalls = listOf(AiToolNames.ADD_DEVICE to mapOf(AiToolParams.NAME to "Porch Light"))
            val action = h.session
                .send("add it")
                .toList()
                .single()
                .actions
                .single()
            h.engine.scriptedToolCalls = listOf(AiToolNames.DELETE_DEVICE to mapOf(AiToolParams.ID to "d1"))
            h.session.send("delete it").toList()
            h.engine.scriptedToolCalls = emptyList()

            h.session.reset()
            assertEquals(1, h.engine.resetCount)
            assertNull(h.session.pendingAction.value)
            assertIs<Result.Error<*>>(h.session.undo(action.id))
            h.session.send("hi").toList()
            assertEquals("hi", h.engine.recordedPrompts.last(), "no notes survive a reset")
        }
}
