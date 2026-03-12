package com.chriscartland.batterybutler.viewmodel.aichat

import com.chriscartland.batterybutler.domain.model.ai.AiRole
import com.chriscartland.batterybutler.testcommon.FakeAiEngine
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.usecase.BuildAiContextUseCase
import com.chriscartland.batterybutler.usecase.DeviceToolHandler
import com.chriscartland.batterybutler.usecase.FindOrCreateDeviceTypeUseCase
import com.chriscartland.batterybutler.usecase.FindOrCreateDeviceUseCase
import com.chriscartland.batterybutler.usecase.SendChatMessageUseCase
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

    @Test
    fun `sendMessage adds user message to messages flow`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.sendMessage("Hello AI")
            // User message is added synchronously before the coroutine launches
            val messages = viewModel.messages.value
            assertEquals(1, messages.size)
            assertEquals(AiRole.USER, messages[0].role)
            assertTrue(messages[0].text == "Hello AI")
        }

    @Test
    fun `sendMessage adds AI response after user message`() =
        runTest {
            val engine = FakeAiEngine()
            engine.defaultResponseText = "Hello human!"
            val viewModel = createViewModel(engine = engine)

            viewModel.sendMessage("Hello AI")
            testDispatcher.scheduler.advanceUntilIdle()

            val messages = viewModel.messages.value
            assertEquals(2, messages.size)
            assertEquals(AiRole.USER, messages[0].role)
            assertEquals(AiRole.MODEL, messages[1].role)
            assertEquals("Hello human!", messages[1].text)
        }

    @Test
    fun `sendMessage ignores blank input`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.sendMessage("")
            viewModel.sendMessage("   ")

            assertTrue(viewModel.messages.value.isEmpty())
        }

    @Test
    fun `sendMessage ignores input while processing`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.sendMessage("First message")
            // isProcessing is true now, before the coroutine completes
            assertTrue(viewModel.isProcessing.value)

            viewModel.sendMessage("Second message")
            // Second message should be ignored since we're still processing
            val userMessages = viewModel.messages.value.filter { it.role == AiRole.USER }
            assertEquals(1, userMessages.size)
        }

    @Test
    fun `isProcessing is true during AI call and false after`() =
        runTest {
            val viewModel = createViewModel()

            assertFalse(viewModel.isProcessing.value)

            viewModel.sendMessage("Hello")
            assertTrue(viewModel.isProcessing.value)

            testDispatcher.scheduler.advanceUntilIdle()
            assertFalse(viewModel.isProcessing.value)
        }

    @Test
    fun `clearChat resets messages and processing state`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.sendMessage("Hello")
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(viewModel.messages.value.isNotEmpty())

            viewModel.clearChat()

            assertTrue(viewModel.messages.value.isEmpty())
            assertFalse(viewModel.isProcessing.value)
        }

    @Test
    fun `sendMessage with hints augments message text`() =
        runTest {
            val engine = FakeAiEngine()
            engine.defaultResponseText = "Got it"
            val viewModel = createViewModel(engine = engine)

            viewModel.sendMessage("Help", hints = mapOf("Active tab" to "Devices"))
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(engine.recordedPrompts.isNotEmpty())
            val sentPrompt = engine.recordedPrompts[0]
            assertTrue(sentPrompt.contains("[Active tab: Devices]"))
            assertTrue(sentPrompt.contains("Help"))
        }

    private fun createViewModel(engine: FakeAiEngine = FakeAiEngine()): AiChatViewModel {
        val repo = FakeDeviceRepository()
        val findOrCreateType = FindOrCreateDeviceTypeUseCase(repo)
        val findOrCreateDevice = FindOrCreateDeviceUseCase(repo, findOrCreateType)
        val updateLastReplaced = UpdateDeviceLastReplacedUseCase(repo)
        val toolHandler = DeviceToolHandler(repo, findOrCreateType, findOrCreateDevice, updateLastReplaced)
        val buildAiContext = BuildAiContextUseCase(repo)
        val sendChatMessage = SendChatMessageUseCase(engine, toolHandler, buildAiContext)
        return AiChatViewModel(sendChatMessage)
    }
}
