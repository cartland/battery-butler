package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.BatchOperationResult
import com.chriscartland.batterybutler.domain.model.ai.AiEngine
import com.chriscartland.batterybutler.domain.model.ai.AiMessage
import com.chriscartland.batterybutler.domain.model.ai.AiRole
import com.chriscartland.batterybutler.domain.model.ai.ToolHandler
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BatchAddDeviceTypesUseCaseTest {
    @Test
    fun `invoke adds device type via tool handler`() =
        runTest {
            val repo = FakeDeviceRepository()
            val engine = FakeAiEngineForDeviceTypes()
            val useCase = BatchAddDeviceTypesUseCase(engine, repo)

            val results = useCase("Add Smoke Detector type").toList()

            assertEquals(1, repo.deviceTypes.size)
            assertEquals("Smoke Detector", repo.deviceTypes[0].name)
            assertEquals("detector_smoke", repo.deviceTypes[0].defaultIcon)
            assertEquals("9V", repo.deviceTypes[0].batteryType)
            assertEquals(1, repo.deviceTypes[0].batteryQuantity)
            val progress = results.filterIsInstance<BatchOperationResult.Progress>().first()
            assertTrue(progress.message.contains("Success"))
        }

    @Test
    fun `invoke deduplicates existing types by name`() =
        runTest {
            val repo = FakeDeviceRepository()
            val existingType = TestDevices.createDeviceType(id = "existing-1", name = "Smoke Detector")
            repo.setDeviceTypes(listOf(existingType))
            val engine = FakeAiEngineForDeviceTypes()
            val useCase = BatchAddDeviceTypesUseCase(engine, repo)

            val results = useCase("Add Smoke Detector type").toList()

            // Should NOT add a duplicate — still just 1 type
            assertEquals(1, repo.deviceTypes.size)
            val progress = results.filterIsInstance<BatchOperationResult.Progress>().first()
            assertTrue(progress.message.contains("already exists"))
        }

    @Test
    fun `invoke handles missing name gracefully`() =
        runTest {
            val repo = FakeDeviceRepository()
            val engine = FakeAiEngineForDeviceTypesMissingName()
            val useCase = BatchAddDeviceTypesUseCase(engine, repo)

            val results = useCase("Add some type").toList()

            assertEquals(0, repo.deviceTypes.size)
            val progress = results.filterIsInstance<BatchOperationResult.Progress>().first()
            assertTrue(progress.message.contains("Error"))
        }
}

/**
 * Fake AI engine that invokes the addDeviceType tool with valid parameters.
 */
private class FakeAiEngineForDeviceTypes : AiEngine {
    override val isAvailable: Flow<Boolean> = flowOf(true)
    override val compatibility: Flow<Boolean> = flowOf(true)

    override suspend fun generateResponse(
        prompt: String,
        toolHandler: ToolHandler?,
    ): Flow<AiMessage> {
        val responseText = if (toolHandler != null) {
            toolHandler.execute(
                "addDeviceType",
                mapOf(
                    "name" to "Smoke Detector",
                    "icon" to "detector_smoke",
                    "batteryType" to "9V",
                    "batteryQuantity" to "1",
                ),
            )
        } else {
            "No Tool"
        }
        return flowOf(AiMessage(id = "1", role = AiRole.MODEL, text = responseText))
    }
}

/**
 * Fake AI engine that invokes the addDeviceType tool without the required name parameter.
 */
private class FakeAiEngineForDeviceTypesMissingName : AiEngine {
    override val isAvailable: Flow<Boolean> = flowOf(true)
    override val compatibility: Flow<Boolean> = flowOf(true)

    override suspend fun generateResponse(
        prompt: String,
        toolHandler: ToolHandler?,
    ): Flow<AiMessage> {
        val responseText = if (toolHandler != null) {
            toolHandler.execute("addDeviceType", mapOf("icon" to "default"))
        } else {
            "No Tool"
        }
        return flowOf(AiMessage(id = "1", role = AiRole.MODEL, text = responseText))
    }
}
