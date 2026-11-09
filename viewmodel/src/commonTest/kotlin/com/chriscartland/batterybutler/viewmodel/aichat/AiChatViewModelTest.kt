package com.chriscartland.batterybutler.viewmodel.aichat

import com.chriscartland.batterybutler.domain.model.ai.AiCardStatus
import com.chriscartland.batterybutler.domain.model.ai.AiRole
import com.chriscartland.batterybutler.domain.model.ai.AiToolNames
import com.chriscartland.batterybutler.domain.model.ai.AiToolParams
import com.chriscartland.batterybutler.testcommon.FakeAiEngine
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.FakeNeedsBatteryRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import com.chriscartland.batterybutler.usecase.AiChatSession
import com.chriscartland.batterybutler.usecase.BuildAiContextUseCase
import com.chriscartland.batterybutler.usecase.DeviceReadTools
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

    private val addPorch = AiToolNames.ADD_DEVICE to mapOf<String, Any?>(AiToolParams.NAME to "Porch Light")

    @Test
    fun `sendMessage adds the user message at once and the reply after`() =
        runTest {
            val h = Harness()
            h.engine.defaultResponseText = "Hello human!"
            h.viewModel.sendMessage("Hello AI")
            assertEquals(
                listOf(AiRole.USER),
                h.viewModel.messages.value
                    .map { it.role },
            )
            settle()
            assertEquals(
                listOf(AiRole.USER, AiRole.MODEL),
                h.viewModel.messages.value
                    .map { it.role },
            )
            assertEquals(
                "Hello human!",
                h.viewModel.messages.value[1]
                    .text,
            )
        }

    @Test
    fun `sendMessage ignores blank input and input while processing`() =
        runTest {
            val h = Harness()
            h.viewModel.sendMessage("   ")
            assertTrue(
                h.viewModel.messages.value
                    .isEmpty(),
            )
            h.viewModel.sendMessage("First")
            assertTrue(h.viewModel.isProcessing.value)
            h.viewModel.sendMessage("Second")
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
    fun `a proposal reaches the screen as a card and nothing changes until approved`() =
        runTest {
            val h = Harness()
            h.engine.scriptedToolCalls = listOf(addPorch)
            h.viewModel.sendMessage("add a porch light")
            settle()
            val cardId = assertNotNull(
                h.viewModel.messages.value
                    .last()
                    .cardId,
            )
            assertEquals(
                AiCardStatus.PENDING,
                h.viewModel.cards.value[cardId]
                    ?.status,
            )
            assertEquals(1, h.repo.devices.size)

            h.viewModel.approveCard(cardId)
            settle()
            assertEquals(
                AiCardStatus.APPLIED,
                h.viewModel.cards.value[cardId]
                    ?.status,
            )
            assertEquals(2, h.repo.devices.size)

            h.viewModel.undoCard(cardId)
            settle()
            assertEquals(
                AiCardStatus.UNDONE,
                h.viewModel.cards.value[cardId]
                    ?.status,
            )
            assertEquals(1, h.repo.devices.size)
        }

    @Test
    fun `declining leaves the data alone`() =
        runTest {
            val h = Harness()
            h.engine.scriptedToolCalls = listOf(AiToolNames.DELETE_DEVICE to mapOf(AiToolParams.ID to "d1"))
            h.viewModel.sendMessage("delete it")
            settle()
            val cardId = assertNotNull(
                h.viewModel.messages.value
                    .last()
                    .cardId,
            )
            h.viewModel.declineCard(cardId)
            settle()
            assertEquals(
                AiCardStatus.DECLINED,
                h.viewModel.cards.value[cardId]
                    ?.status,
            )
            assertEquals(1, h.repo.devices.size)
        }

    @Test
    fun `a failed reply can be retried with the same words`() =
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
            assertEquals(
                listOf(AiRole.USER, AiRole.MODEL),
                h.viewModel.messages.value
                    .map { it.role },
            )
            assertEquals(
                "Back.",
                h.viewModel.messages.value
                    .last()
                    .text,
            )
            assertEquals(h.engine.recordedPrompts[0], h.engine.recordedPrompts[1])
        }

    @Test
    fun `retry is refused when the failed reply left a card`() =
        runTest {
            val h = Harness()
            h.engine.scriptedToolCalls = listOf(addPorch)
            h.engine.errorAfterToolCalls = IllegalStateException("boom")
            h.viewModel.sendMessage("add it")
            settle()
            val before = h.viewModel.messages.value
            h.engine.errorAfterToolCalls = null
            h.viewModel.retryLastMessage()
            settle()
            assertEquals(before, h.viewModel.messages.value)
        }

    @Test
    fun `clearChat resets the transcript the cards and the conversation`() =
        runTest {
            val h = Harness()
            h.engine.scriptedToolCalls = listOf(addPorch)
            h.viewModel.sendMessage("add it")
            settle()
            h.viewModel.clearChat()
            settle()
            assertTrue(
                h.viewModel.messages.value
                    .isEmpty(),
            )
            assertTrue(
                h.viewModel.cards.value
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
        private val marks = FakeNeedsBatteryRepository()
        val viewModel = AiChatViewModel(
            AiChatSession(
                aiEngine = engine,
                writer = DeviceToolHandler(repo, marks, UpdateDeviceLastReplacedUseCase(repo)),
                reader = DeviceReadTools(repo, marks),
                buildAiContextUseCase = BuildAiContextUseCase(repo, marks),
            ),
        )
    }
}
