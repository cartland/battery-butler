package com.chriscartland.batterybutler.viewmodel.aichat

import com.chriscartland.batterybutler.domain.model.ai.AiRole
import com.chriscartland.batterybutler.domain.model.ai.AiToolNames
import com.chriscartland.batterybutler.domain.model.ai.AiToolParams
import com.chriscartland.batterybutler.testcommon.FakeAiEngine
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.FakeNeedsBatteryRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import com.chriscartland.batterybutler.usecase.AiChatSession
import com.chriscartland.batterybutler.usecase.BuildAiContextUseCase
import com.chriscartland.batterybutler.usecase.DeviceToolHandler
import com.chriscartland.batterybutler.usecase.UpdateDeviceLastReplacedUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AiChatViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun settle() = testDispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `sendMessage adds user message to messages flow`() =
        runTest {
            val h = Harness()
            h.viewModel.sendMessage("Hello AI")
            // User message is added synchronously before the coroutine launches
            val messages = h.viewModel.messages.value
            assertEquals(1, messages.size)
            assertEquals(AiRole.USER, messages[0].role)
            assertEquals("Hello AI", messages[0].text)
        }

    @Test
    fun `sendMessage adds AI response after user message`() =
        runTest {
            val h = Harness()
            h.engine.defaultResponseText = "Hello human!"
            h.viewModel.sendMessage("Hello AI")
            settle()
            val messages = h.viewModel.messages.value
            assertEquals(2, messages.size)
            assertEquals(AiRole.MODEL, messages[1].role)
            assertEquals("Hello human!", messages[1].text)
        }

    @Test
    fun `sendMessage ignores blank input and input while processing`() =
        runTest {
            val h = Harness()
            h.viewModel.sendMessage("")
            h.viewModel.sendMessage("   ")
            assertTrue(
                h.viewModel.messages.value
                    .isEmpty(),
            )

            h.viewModel.sendMessage("First message")
            assertTrue(h.viewModel.isProcessing.value)
            h.viewModel.sendMessage("Second message")
            assertEquals(
                1,
                h.viewModel.messages.value
                    .count { it.role == AiRole.USER },
            )
            settle()
            assertFalse(h.viewModel.isProcessing.value)
        }

    @Test
    fun `hints reach the model as bracketed lines`() =
        runTest {
            val h = Harness()
            h.viewModel.sendMessage("Help", hints = mapOf("Message sent from" to "Devices tab"))
            settle()
            assertEquals("[Message sent from: Devices tab]\n\nHelp", h.engine.recordedPrompts.single())
        }

    @Test
    fun `a reply carries the actions taken and and undo marks the row undone`() =
        runTest {
            val h = Harness()
            h.engine.scriptedToolCalls = listOf(AiToolNames.ADD_DEVICE to mapOf(AiToolParams.NAME to "Porch Light"))
            h.viewModel.sendMessage("add a porch light")
            settle()
            val action = h.viewModel.messages.value
                .last()
                .actions
                .single()
            assertTrue(action.undoable && !action.undone)
            assertEquals(2, h.repo.devices.size)

            h.viewModel.undoAction(action.id)
            settle()
            val after = h.viewModel.messages.value
                .last()
                .actions
                .single()
            assertTrue(after.undone)
            assertFalse(after.undoable)
            assertEquals(1, h.repo.devices.size)
        }

    @Test
    fun `a deletion shows up as a pending action until the person decides`() =
        runTest {
            val h = Harness()
            h.engine.scriptedToolCalls = listOf(AiToolNames.DELETE_DEVICE to mapOf(AiToolParams.ID to "d1"))
            h.viewModel.sendMessage("delete the hall alarm")
            settle()
            val pending = assertNotNull(h.viewModel.pendingAction.value)
            assertEquals("Delete Hall Alarm", pending.summary)
            assertEquals(1, h.repo.devices.size)

            h.viewModel.confirmPendingAction()
            settle()
            assertNull(h.viewModel.pendingAction.value)
            assertTrue(h.repo.devices.isEmpty())
            val entry = h.viewModel.messages.value
                .last()
            assertEquals(AiRole.SYSTEM, entry.role)
            assertEquals("", entry.text)
            assertEquals("Deleted Hall Alarm", entry.actions.single().summary)
            assertTrue(entry.actions.single().undoable)
        }

    @Test
    fun `keeping a pending deletion deletes nothing and adds no entry`() =
        runTest {
            val h = Harness()
            h.engine.scriptedToolCalls = listOf(AiToolNames.DELETE_DEVICE to mapOf(AiToolParams.ID to "d1"))
            h.viewModel.sendMessage("delete it")
            settle()
            val before = h.viewModel.messages.value.size
            h.viewModel.cancelPendingAction()
            settle()
            assertNull(h.viewModel.pendingAction.value)
            assertEquals(1, h.repo.devices.size)
            assertEquals(before, h.viewModel.messages.value.size)
        }

    @Test
    fun `a failed reply is an error entry and and retry sends the same words again`() =
        runTest {
            val h = Harness()
            h.engine.errorToThrow = IllegalStateException("boom")
            h.viewModel.sendMessage("Hello", hints = mapOf("k" to "v"))
            settle()
            val error = h.viewModel.messages.value
                .last()
            assertTrue(error.isError)
            assertTrue(error.text.contains("Nothing in your data was changed"))

            h.engine.errorToThrow = null
            h.engine.defaultResponseText = "Back."
            h.viewModel.retryLastMessage()
            settle()
            val messages = h.viewModel.messages.value
            assertEquals(listOf(AiRole.USER, AiRole.MODEL), messages.map { it.role })
            assertEquals("Back.", messages.last().text)
            assertEquals(2, h.engine.recordedPrompts.size)
            assertEquals(h.engine.recordedPrompts[0], h.engine.recordedPrompts[1])
        }

    @Test
    fun `retry is refused after a failed turn that made changes`() =
        runTest {
            val h = Harness()
            h.engine.scriptedToolCalls = listOf(AiToolNames.RECORD_BATTERY_REPLACEMENT to mapOf(AiToolParams.DEVICE_NAME to "Hall Alarm"))
            h.engine.errorAfterToolCalls = IllegalStateException("boom")
            h.viewModel.sendMessage("I changed it")
            settle()
            assertEquals(1, h.repo.events.size)
            val before = h.viewModel.messages.value

            h.engine.errorAfterToolCalls = null
            h.viewModel.retryLastMessage()
            settle()
            assertEquals(1, h.repo.events.size, "a retry must not record it twice")
            assertEquals(before, h.viewModel.messages.value, "the row and its Undo stay")
        }

    @Test
    fun `clearChat resets the transcript and the conversation behind it`() =
        runTest {
            val h = Harness()
            h.viewModel.sendMessage("Hello")
            settle()
            assertTrue(
                h.viewModel.messages.value
                    .isNotEmpty(),
            )

            h.viewModel.clearChat()
            settle()
            assertTrue(
                h.viewModel.messages.value
                    .isEmpty(),
            )
            assertFalse(h.viewModel.isProcessing.value)
            assertEquals(1, h.engine.resetCount)
        }

    private class Harness {
        val repo = FakeDeviceRepository().apply {
            setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Hall Alarm")))
        }
        val engine = FakeAiEngine()
        val viewModel = AiChatViewModel(
            AiChatSession(
                aiEngine = engine,
                toolHandler = DeviceToolHandler(repo, FakeNeedsBatteryRepository(), UpdateDeviceLastReplacedUseCase(repo)),
                buildAiContextUseCase = BuildAiContextUseCase(repo, FakeNeedsBatteryRepository()),
            ),
        )
    }
}
