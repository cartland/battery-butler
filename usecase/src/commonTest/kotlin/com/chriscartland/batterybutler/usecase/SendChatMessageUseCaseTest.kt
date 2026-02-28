package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.testcommon.FakeAiEngine
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class SendChatMessageUseCaseTest {
    private fun createUseCase(
        repo: FakeDeviceRepository = FakeDeviceRepository(),
        engine: FakeAiEngine = FakeAiEngine(),
    ): Pair<SendChatMessageUseCase, FakeAiEngine> {
        val findOrCreateType = FindOrCreateDeviceTypeUseCase(repo)
        val findOrCreateDevice = FindOrCreateDeviceUseCase(repo, findOrCreateType)
        val toolHandler = DeviceToolHandler(repo, findOrCreateType, findOrCreateDevice)
        val buildContext = BuildAiContextUseCase(repo)
        val useCase = SendChatMessageUseCase(engine, toolHandler, buildContext)
        return useCase to engine
    }

    @Test
    fun `augments message with time context`() =
        runTest {
            val (useCase, engine) = createUseCase()

            useCase("Hello")

            assertEquals(1, engine.recordedPrompts.size)
            assertTrue(engine.recordedPrompts[0].contains("[Context: Current date/time:"))
        }

    @Test
    fun `augments message with device inventory`() =
        runTest {
            val (useCase, engine) = createUseCase()

            useCase("Hello")

            assertEquals(1, engine.recordedPrompts.size)
            assertTrue(engine.recordedPrompts[0].contains("User's Inventory"))
        }

    @Test
    fun `passes original message through`() =
        runTest {
            val (useCase, engine) = createUseCase()

            useCase("What batteries do I need?")

            assertEquals(1, engine.recordedPrompts.size)
            assertTrue(engine.recordedPrompts[0].contains("What batteries do I need?"))
        }

    @Test
    fun `returns engine flow`() =
        runTest {
            val engine = FakeAiEngine()
            engine.defaultResponseText = "I can help with that"
            val (useCase, _) = createUseCase(engine = engine)

            val result = useCase("Hi").first()

            assertEquals("I can help with that", result.text)
        }
}
