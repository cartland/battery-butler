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
import kotlin.test.assertEquals

// Fake implementations for testing
class FakeDeviceRepository : DeviceRepository {
    val devices = mutableListOf<Device>()
    val deviceTypes = mutableListOf<DeviceType>()
    override val syncStatus: StateFlow<SyncStatus> = MutableStateFlow(SyncStatus.Idle)

    override fun dismissSyncStatus() {}

    override fun getAllDevices(): Flow<List<Device>> = flowOf(devices)

    override fun getDeviceById(id: String): Flow<Device?> = flowOf(devices.find { it.id == id })

    override suspend fun addDevice(device: Device) {
        devices.add(device)
    }

    override suspend fun updateDevice(device: Device) {
        devices.removeAll { it.id == device.id }
        devices.add(device)
    }

    override suspend fun deleteDevice(id: String) {
        devices.removeAll { it.id == id }
    }

    override fun getAllDeviceTypes(): Flow<List<DeviceType>> = flowOf(deviceTypes)

    override fun getDeviceTypeById(id: String): Flow<DeviceType?> = flowOf(deviceTypes.find { it.id == id })

    override suspend fun addDeviceType(type: DeviceType) {
        deviceTypes.add(type)
    }

    override suspend fun updateDeviceType(type: DeviceType) {
        deviceTypes.removeAll { it.id == type.id }
        deviceTypes.add(type)
    }

    override suspend fun deleteDeviceType(id: String) {
        deviceTypes.removeAll { it.id == id }
    }

    override fun getEventsForDevice(deviceId: String): Flow<List<BatteryEvent>> = flowOf(emptyList())

    override fun getAllEvents(): Flow<List<BatteryEvent>> = flowOf(emptyList())

    override fun getEventById(id: String): Flow<BatteryEvent?> = flowOf(null)

    override suspend fun addEvent(event: BatteryEvent) {}

    override suspend fun updateEvent(event: BatteryEvent) {}

    override suspend fun deleteEvent(id: String) {}
}

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
