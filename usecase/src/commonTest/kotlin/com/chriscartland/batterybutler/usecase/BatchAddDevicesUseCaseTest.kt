package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.ai.AiEngine
import com.chriscartland.batterybutler.ai.AiMessage
import com.chriscartland.batterybutler.ai.AiRole
import com.chriscartland.batterybutler.ai.ToolHandler
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FakeAiEngine : AiEngine {
    override val isAvailable: Flow<Boolean> = flowOf(true)
    override val compatibility: Flow<Boolean> = flowOf(true)

    override suspend fun generateResponse(
        prompt: String,
        toolHandler: ToolHandler?,
    ): Flow<AiMessage> {
        val responseText = if (toolHandler != null) {
            toolHandler.execute("addDevice", mapOf("name" to "Test Device", "type" to "Test Type"))
        } else {
            "No Tool"
        }

        return flowOf(
            AiMessage(
                id = "1",
                role = AiRole.MODEL,
                text = responseText,
            ),
        )
    }
}

class BatchAddDevicesUseCaseTest {
    @Test
    fun invoke_adds_device_and_type_correctly() =
        runTest {
            val repo = FakeDeviceRepository()
            val engine = FakeAiEngine()
            val useCase = BatchAddDevicesUseCase(engine, repo)

            val results = useCase("Add Test Device").toList()

            assertEquals(1, repo.devices.size)
            assertEquals("Test Device", repo.devices[0].name)
            assertEquals(1, repo.deviceTypes.size)
            assertEquals("Test Type", repo.deviceTypes[0].name)
            val progressResult = results
                .filterIsInstance<com.chriscartland.batterybutler.domain.model.BatchOperationResult.Progress>()
                .first()
            assertEquals("Success: Added device 'Test Device' (Type: Test Type)", progressResult.message)
        }
}
