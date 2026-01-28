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
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

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

            val types = viewModel.deviceTypes.first { it.isNotEmpty() }

            assertEquals(1, types.size)
            assertEquals("Test Type", types[0].name)
        }

    @Test
    fun `multiple device types are loaded`() =
        runTest {
            val repo = FakeDeviceRepository()
            val type1 = DeviceType(id = "type-1", name = "Smoke Detector")
            val type2 = DeviceType(id = "type-2", name = "Remote Control")
            val type3 = DeviceType(id = "type-3", name = "Door Lock")
            repo.setDeviceTypes(listOf(type1, type2, type3))

            val viewModel = createViewModel(repo)

            val types = viewModel.deviceTypes.first { it.size == 3 }

            assertEquals(3, types.size)
            assertTrue(types.any { it.name == "Smoke Detector" })
            assertTrue(types.any { it.name == "Remote Control" })
            assertTrue(types.any { it.name == "Door Lock" })
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
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, repo.addedDevices.size)
            assertEquals("New Device", repo.addedDevices[0].name)
            assertEquals("Living Room", repo.addedDevices[0].location)
        }

    @Test
    fun `addDevice generates unique id for each device`() =
        runTest {
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo)
            val input1 = DeviceInput(name = "Device 1", location = "Kitchen", typeId = "type-1")
            val input2 = DeviceInput(name = "Device 2", location = "Bedroom", typeId = "type-1")

            viewModel.addDevice(input1)
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.addDevice(input2)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(2, repo.addedDevices.size)
            assertNotEquals(repo.addedDevices[0].id, repo.addedDevices[1].id)
        }

    @Test
    fun `addDevice preserves typeId`() =
        runTest {
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo)
            val input = DeviceInput(
                name = "Smoke Detector",
                location = "Kitchen",
                typeId = "type-smoke-detector",
            )

            viewModel.addDevice(input)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("type-smoke-detector", repo.addedDevices[0].typeId)
        }

    @Test
    fun `clearAiMessages clears the message list`() =
        runTest {
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo)

            // Initial state should be empty
            val initialMessages = viewModel.aiMessages.first()
            assertTrue(initialMessages.isEmpty())

            // Clear should work even when empty
            viewModel.clearAiMessages()
            val afterClear = viewModel.aiMessages.first()
            assertTrue(afterClear.isEmpty())
        }

    @Test
    fun `deviceTypes initial state is empty list`() =
        runTest {
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo)

            // Initial value is empty list before any updates
            val types = viewModel.deviceTypes.value
            assertTrue(types.isEmpty())
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

    override fun dismissSyncStatus() {}

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
