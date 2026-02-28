package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.BatchOperationResult
import com.chriscartland.batterybutler.domain.model.ai.AiEngine
import com.chriscartland.batterybutler.domain.model.ai.AiMessage
import com.chriscartland.batterybutler.domain.model.ai.AiRole
import com.chriscartland.batterybutler.domain.model.ai.ToolHandler
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BatchAddBatteryEventsUseCaseTest {
    @Test
    fun `invoke creates device and event via tool handler`() =
        runTest {
            val repo = FakeDeviceRepository()
            val engine = FakeAiEngineForBatteryEvents()
            val findOrCreateType = FindOrCreateDeviceTypeUseCase(repo)
            val findOrCreateDevice = FindOrCreateDeviceUseCase(repo, findOrCreateType)
            val updateLastReplaced = UpdateDeviceLastReplacedUseCase(repo)
            val addBatteryEvent = AddBatteryEventUseCase(repo, updateLastReplaced)
            val useCase = BatchAddBatteryEventsUseCase(engine, addBatteryEvent, findOrCreateDevice)

            val results = useCase("Replaced smoke detector batteries on 2025-03-15").toList()

            // Device should have been created (via FindOrCreateDeviceUseCase)
            assertEquals(1, repo.devices.size)
            assertEquals("Smoke Detector", repo.devices[0].name)
            // Event should have been added
            assertEquals(1, repo.events.size)
            assertEquals(repo.devices[0].id, repo.events[0].deviceId)
            val progress = results.filterIsInstance<BatchOperationResult.Progress>().first()
            assertTrue(progress.message.contains("Success"))
        }

    @Test
    fun `invoke handles missing deviceName gracefully`() =
        runTest {
            val repo = FakeDeviceRepository()
            val engine = FakeAiEngineForBatteryEventsMissingDevice()
            val findOrCreateType = FindOrCreateDeviceTypeUseCase(repo)
            val findOrCreateDevice = FindOrCreateDeviceUseCase(repo, findOrCreateType)
            val updateLastReplaced = UpdateDeviceLastReplacedUseCase(repo)
            val addBatteryEvent = AddBatteryEventUseCase(repo, updateLastReplaced)
            val useCase = BatchAddBatteryEventsUseCase(engine, addBatteryEvent, findOrCreateDevice)

            val results = useCase("Some event").toList()

            assertEquals(0, repo.events.size)
            val progress = results.filterIsInstance<BatchOperationResult.Progress>().first()
            assertTrue(progress.message.contains("Error"))
        }

    @Test
    fun `invoke handles missing date gracefully`() =
        runTest {
            val repo = FakeDeviceRepository()
            val engine = FakeAiEngineForBatteryEventsMissingDate()
            val findOrCreateType = FindOrCreateDeviceTypeUseCase(repo)
            val findOrCreateDevice = FindOrCreateDeviceUseCase(repo, findOrCreateType)
            val updateLastReplaced = UpdateDeviceLastReplacedUseCase(repo)
            val addBatteryEvent = AddBatteryEventUseCase(repo, updateLastReplaced)
            val useCase = BatchAddBatteryEventsUseCase(engine, addBatteryEvent, findOrCreateDevice)

            val results = useCase("Some event").toList()

            assertEquals(0, repo.events.size)
            val progress = results.filterIsInstance<BatchOperationResult.Progress>().first()
            assertTrue(progress.message.contains("Error"))
        }
}

/**
 * Fake AI engine that invokes the recordBatteryReplacement tool with valid parameters.
 */
private class FakeAiEngineForBatteryEvents : AiEngine {
    override val isAvailable: Flow<Boolean> = flowOf(true)
    override val compatibility: Flow<Boolean> = flowOf(true)

    override suspend fun generateResponse(
        prompt: String,
        toolHandler: ToolHandler?,
    ): Flow<AiMessage> {
        val responseText = if (toolHandler != null) {
            toolHandler.execute(
                "recordBatteryReplacement",
                mapOf(
                    "deviceName" to "Smoke Detector",
                    "date" to "2025-03-15",
                ),
            )
        } else {
            "No Tool"
        }
        return flowOf(AiMessage(id = "1", role = AiRole.MODEL, text = responseText))
    }
}

/**
 * Fake AI engine that invokes the recordBatteryReplacement tool without the required deviceName.
 */
private class FakeAiEngineForBatteryEventsMissingDevice : AiEngine {
    override val isAvailable: Flow<Boolean> = flowOf(true)
    override val compatibility: Flow<Boolean> = flowOf(true)

    override suspend fun generateResponse(
        prompt: String,
        toolHandler: ToolHandler?,
    ): Flow<AiMessage> {
        val responseText = if (toolHandler != null) {
            toolHandler.execute(
                "recordBatteryReplacement",
                mapOf("date" to "2025-03-15"),
            )
        } else {
            "No Tool"
        }
        return flowOf(AiMessage(id = "1", role = AiRole.MODEL, text = responseText))
    }
}

/**
 * Fake AI engine that invokes the recordBatteryReplacement tool without the required date.
 */
private class FakeAiEngineForBatteryEventsMissingDate : AiEngine {
    override val isAvailable: Flow<Boolean> = flowOf(true)
    override val compatibility: Flow<Boolean> = flowOf(true)

    override suspend fun generateResponse(
        prompt: String,
        toolHandler: ToolHandler?,
    ): Flow<AiMessage> {
        val responseText = if (toolHandler != null) {
            toolHandler.execute(
                "recordBatteryReplacement",
                mapOf("deviceName" to "Smoke Detector"),
            )
        } else {
            "No Tool"
        }
        return flowOf(AiMessage(id = "1", role = AiRole.MODEL, text = responseText))
    }
}
