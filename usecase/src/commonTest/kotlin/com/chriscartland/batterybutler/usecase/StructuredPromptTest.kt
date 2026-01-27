package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.ai.AiEngine
import com.chriscartland.batterybutler.ai.AiMessage
import com.chriscartland.batterybutler.ai.AiRole
import com.chriscartland.batterybutler.ai.ToolHandler
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.SyncStatus
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class StructuredPromptTest {
    class CapturingAiEngine : AiEngine {
        var lastPrompt: String? = null
        override val isAvailable: Flow<Boolean> = flowOf(true)
        override val compatibility: Flow<Boolean> = flowOf(true)

        override suspend fun generateResponse(
            prompt: String,
            toolHandler: ToolHandler?,
        ): Flow<AiMessage> {
            lastPrompt = prompt
            return flowOf(AiMessage(id = "1", role = AiRole.MODEL, text = "Mock response"))
        }
    }

    class MockRepository : DeviceRepository {
        override val syncStatus: StateFlow<SyncStatus> = MutableStateFlow(SyncStatus.Idle)

        override fun dismissSyncStatus() {}

        override fun getAllDevices() = flowOf(emptyList<Device>())

        override fun getDeviceById(id: String) = flowOf(null)

        override suspend fun addDevice(device: Device) {}

        override suspend fun updateDevice(device: Device) {}

        override suspend fun deleteDevice(id: String) {}

        override fun getAllDeviceTypes() = flowOf(emptyList<DeviceType>())

        override fun getDeviceTypeById(id: String) = flowOf(null)

        override suspend fun addDeviceType(type: DeviceType) {}

        override suspend fun updateDeviceType(type: DeviceType) {}

        override suspend fun deleteDeviceType(id: String) {}

        override fun getEventsForDevice(deviceId: String) = flowOf(emptyList<BatteryEvent>())

        override fun getAllEvents() = flowOf(emptyList<BatteryEvent>())

        override fun getEventById(id: String) = flowOf(null)

        override suspend fun addEvent(event: BatteryEvent) {}

        override suspend fun updateEvent(event: BatteryEvent) {}

        override suspend fun deleteEvent(id: String) {}
    }

    @Test
    fun batchAddDeviceTypesUseCase_usesStructuredPrompt() =
        runTest {
            val engine = CapturingAiEngine()
            val repo = MockRepository()
            val useCase = BatchAddDeviceTypesUseCase(engine, repo)

            useCase("AA Batteries").toList()

            val prompt = engine.lastPrompt ?: ""

            assertTrue(prompt.contains("*** SYSTEM INSTRUCTIONS ***"), "Prompt should contain system instructions header")
            assertTrue(prompt.contains("*** USER INPUT DATA ***"), "Prompt should contain user input header")
            assertTrue(prompt.contains("AA Batteries"), "Prompt should contain user input")
            assertTrue(prompt.contains("Ignore header rows"), "Prompt should contain specific instructions. Actual prompt: $prompt")
        }

    @Test
    fun batchAddDevicesUseCase_usesStructuredPrompt() =
        runTest {
            val engine = CapturingAiEngine()
            val repo = MockRepository()
            val useCase = BatchAddDevicesUseCase(engine, repo)

            useCase("Kitchen Smoke Detector").toList()

            val prompt = engine.lastPrompt ?: ""
            assertTrue(prompt.contains("*** SYSTEM INSTRUCTIONS ***"), "Prompt should contain system instructions header")
            assertTrue(prompt.contains("*** USER INPUT DATA ***"), "Prompt should contain user input header")
            assertTrue(prompt.contains("Kitchen Smoke Detector"), "Prompt should contain user input")
            assertTrue(prompt.contains("Ignore header rows"), "Prompt should contain specific instructions. Actual prompt: $prompt")
        }

    @Test
    fun batchAddBatteryEventsUseCase_usesStructuredPrompt() =
        runTest {
            val engine = CapturingAiEngine()
            val repo = MockRepository()
            // Note: BatchAddBatteryEventsUseCase constructor might be different, checking...
            // It takes AiEngine and DeviceRepository.
            val useCase = BatchAddBatteryEventsUseCase(engine, repo)

            useCase("2023-01-01 Remote").toList()

            val prompt = engine.lastPrompt ?: ""
            assertTrue(prompt.contains("*** SYSTEM INSTRUCTIONS ***"), "Prompt should contain system instructions header")
            assertTrue(prompt.contains("*** USER INPUT DATA ***"), "Prompt should contain user input header")
            assertTrue(prompt.contains("2023-01-01 Remote"), "Prompt should contain user input")
            assertTrue(prompt.contains("Ignore header rows"), "Prompt should contain specific instructions. Actual prompt: $prompt")
        }

    @Test
    fun suggestDeviceIconUseCase_usesStructuredPrompt() =
        runTest {
            val engine = CapturingAiEngine()
            val useCase = SuggestDeviceIconUseCase(engine)

            useCase("Xbox Controller")

            val prompt = engine.lastPrompt ?: ""
            assertTrue(prompt.contains("*** SYSTEM INSTRUCTIONS ***"), "Prompt should contain system instructions header")
            assertTrue(prompt.contains("*** USER INPUT DATA ***"), "Prompt should contain user input header")
            assertTrue(prompt.contains("Xbox Controller"), "Prompt should contain user input")
            assertTrue(prompt.contains("Available Icons"), "Prompt should list available icons")
        }
}
