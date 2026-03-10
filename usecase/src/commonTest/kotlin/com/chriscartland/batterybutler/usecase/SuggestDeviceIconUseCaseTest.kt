package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.testcommon.FakeAiEngine
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SuggestDeviceIconUseCaseTest {
    @Test
    fun `sends prompt with available icons`() =
        runTest {
            val engine = FakeAiEngine()
            val useCase = SuggestDeviceIconUseCase(engine)

            useCase("Smoke Detector")

            assertEquals(1, engine.recordedPrompts.size)
            assertTrue(engine.recordedPrompts[0].contains("Available Icons:"))
        }

    @Test
    fun `sends prompt with device type name`() =
        runTest {
            val engine = FakeAiEngine()
            val useCase = SuggestDeviceIconUseCase(engine)

            useCase("Smoke Detector")

            assertEquals(1, engine.recordedPrompts.size)
            assertTrue(engine.recordedPrompts[0].contains("Smoke Detector"))
        }

    @Test
    fun `returns trimmed response`() =
        runTest {
            val engine = FakeAiEngine()
            engine.defaultResponseText = "  detector_smoke  "
            val useCase = SuggestDeviceIconUseCase(engine)

            val result = useCase("Smoke Detector")

            assertIs<Result.Success<String>>(result)
            assertEquals("detector_smoke", result.data)
        }

    @Test
    fun `returns error on AI failure`() =
        runTest {
            val engine = FakeAiEngine()
            engine.errorToThrow = RuntimeException("API failure")
            val useCase = SuggestDeviceIconUseCase(engine)

            val result = useCase("Smoke Detector")

            assertIs<Result.Error<*>>(result)
            assertTrue(result.error.message.contains("Failed to suggest icon"))
        }
}
