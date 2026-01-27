package com.chriscartland.batterybutler.viewmodel.adddevice

import com.chriscartland.batterybutler.ai.AiEngine
import com.chriscartland.batterybutler.ai.AiMessage
import com.chriscartland.batterybutler.ai.ToolHandler
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceInput
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.SyncStatus
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.usecase.AddDeviceUseCase
import com.chriscartland.batterybutler.usecase.BatchAddDevicesUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AddDeviceViewModelTest {
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
    fun `deviceTypes are loaded from repository`() =
        runTest {
            val repo = FakeDeviceRepository()
            val type = DeviceType(id = "type-1", name = "Test Type")
            repo.setDeviceTypes(listOf(type))

            val viewModel = createViewModel(repo)

            // Initial value is empty, so we wait for first non-empty
            // Or wait for repo update logic to propagate
            val types = viewModel.deviceTypes.first { it.isNotEmpty() }

            assertEquals(1, types.size)
            assertEquals("Test Type", types[0].name)
        }

    @Test
    fun `addDevice adds device to repository`() =
        runTest {
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo)
            val input = DeviceInput(
                name = "New Device",
                location = "Living Room",
                typeId = "type-1",
            )

            viewModel.addDevice(input)

            // Advance dispatcher to execute launch
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, repo.addedDevices.size)
            assertEquals("New Device", repo.addedDevices[0].name)
            assertEquals("Living Room", repo.addedDevices[0].location)
        }

    private fun createViewModel(repo: FakeDeviceRepository): AddDeviceViewModel =
        AddDeviceViewModel(
            addDeviceUseCase = AddDeviceUseCase(repo),
            getDeviceTypesUseCase = GetDeviceTypesUseCase(repo),
            batchAddDevicesUseCase = BatchAddDevicesUseCase(FakeAiEngine(), repo),
        )
}

class FakeDeviceRepository : DeviceRepository {
    private val deviceTypesFlow = MutableStateFlow<List<DeviceType>>(emptyList())
    val addedDevices = mutableListOf<Device>()

    fun setDeviceTypes(types: List<DeviceType>) {
        deviceTypesFlow.value = types
    }

    override val syncStatus: StateFlow<SyncStatus> = MutableStateFlow(SyncStatus.Idle)

    override fun getAllDevices(): Flow<List<Device>> = emptyFlow()

    override fun getDeviceById(id: String): Flow<Device?> = emptyFlow()

    override suspend fun addDevice(device: Device) {
        addedDevices.add(device)
    }

    override suspend fun updateDevice(device: Device) {}

    override suspend fun deleteDevice(id: String) {}

    override fun getAllDeviceTypes(): Flow<List<DeviceType>> = deviceTypesFlow

    override fun getDeviceTypeById(id: String): Flow<DeviceType?> = emptyFlow()

    override suspend fun addDeviceType(type: DeviceType) {}

    override suspend fun updateDeviceType(type: DeviceType) {}

    override suspend fun deleteDeviceType(id: String) {}

    override fun getEventsForDevice(deviceId: String): Flow<List<BatteryEvent>> = emptyFlow()

    override fun getAllEvents(): Flow<List<BatteryEvent>> = emptyFlow()

    override fun getEventById(id: String): Flow<BatteryEvent?> = emptyFlow()

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
    ): Flow<AiMessage> = emptyFlow()
}
