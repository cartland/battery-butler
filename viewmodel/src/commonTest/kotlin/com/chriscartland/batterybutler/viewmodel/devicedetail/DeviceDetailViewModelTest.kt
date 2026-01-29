package com.chriscartland.batterybutler.viewmodel.devicedetail

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.SyncStatus
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.presentationmodel.devicedetail.DeviceDetailUiState
import com.chriscartland.batterybutler.usecase.AddBatteryEventUseCase
import com.chriscartland.batterybutler.usecase.GetBatteryEventsUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceDetailUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.UpdateDeviceUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class DeviceDetailViewModelTest {
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
    fun `initial state is Loading`() =
        runTest {
            val repo = FakeDetailRepository()
            val viewModel = createViewModel(repo, "device-1")

            val state = viewModel.uiState.value

            assertIs<DeviceDetailUiState.Loading>(state)
        }

    @Test
    fun `state becomes NotFound when device does not exist`() =
        runTest {
            val repo = FakeDetailRepository()
            // Don't add any device
            val viewModel = createViewModel(repo, "nonexistent-device")

            advanceUntilIdle()

            val state = viewModel.uiState.first { it !is DeviceDetailUiState.Loading }
            assertIs<DeviceDetailUiState.NotFound>(state)
        }

    @Test
    fun `state becomes Success when device exists`() =
        runTest {
            val repo = FakeDetailRepository()
            val device = createDevice(id = "device-1", name = "Smoke Detector")
            repo.setDevice(device)

            val viewModel = createViewModel(repo, "device-1")

            advanceUntilIdle()

            val state = viewModel.uiState.first { it is DeviceDetailUiState.Success }
            assertIs<DeviceDetailUiState.Success>(state)
            assertEquals("device-1", state.device.id)
            assertEquals("Smoke Detector", state.device.name)
        }

    @Test
    fun `Success state includes device type`() =
        runTest {
            val repo = FakeDetailRepository()
            val device = createDevice(id = "device-1", name = "Smoke Detector", typeId = "type-1")
            val deviceType = DeviceType(id = "type-1", name = "Smoke Detector", batteryType = "9V")
            repo.setDevice(device)
            repo.setDeviceTypes(listOf(deviceType))

            val viewModel = createViewModel(repo, "device-1")

            advanceUntilIdle()

            val state = viewModel.uiState.first { it is DeviceDetailUiState.Success }
            assertIs<DeviceDetailUiState.Success>(state)
            assertNotNull(state.deviceType)
            assertEquals("type-1", state.deviceType!!.id)
            assertEquals("Smoke Detector", state.deviceType!!.name)
            assertEquals("9V", state.deviceType!!.batteryType)
        }

    @Test
    fun `Success state handles missing device type`() =
        runTest {
            val repo = FakeDetailRepository()
            val device = createDevice(id = "device-1", name = "Test Device", typeId = "unknown-type")
            repo.setDevice(device)
            // Don't add the device type

            val viewModel = createViewModel(repo, "device-1")

            advanceUntilIdle()

            val state = viewModel.uiState.first { it is DeviceDetailUiState.Success }
            assertIs<DeviceDetailUiState.Success>(state)
            assertEquals(null, state.deviceType)
        }

    @Test
    fun `Success state includes battery events`() =
        runTest {
            val repo = FakeDetailRepository()
            val device = createDevice(id = "device-1", name = "Test Device")
            val event1 = BatteryEvent(
                id = "event-1",
                deviceId = "device-1",
                date = Instant.parse("2024-01-15T10:00:00Z"),
            )
            val event2 = BatteryEvent(
                id = "event-2",
                deviceId = "device-1",
                date = Instant.parse("2024-01-10T10:00:00Z"),
            )
            repo.setDevice(device)
            repo.setEventsForDevice("device-1", listOf(event1, event2))

            val viewModel = createViewModel(repo, "device-1")

            advanceUntilIdle()

            val state = viewModel.uiState.first { it is DeviceDetailUiState.Success }
            assertIs<DeviceDetailUiState.Success>(state)
            assertEquals(2, state.events.size)
            assertTrue(state.events.any { it.id == "event-1" })
            assertTrue(state.events.any { it.id == "event-2" })
        }

    @Test
    fun `recordReplacement adds battery event`() =
        runTest {
            val repo = FakeDetailRepository()
            val device = createDevice(id = "device-1", name = "Test Device")
            repo.setDevice(device)

            val viewModel = createViewModel(repo, "device-1")

            advanceUntilIdle()

            // Ensure we're in success state first
            viewModel.uiState.first { it is DeviceDetailUiState.Success }

            viewModel.recordReplacement()

            advanceUntilIdle()

            assertEquals(1, repo.addedEvents.size)
            assertEquals("device-1", repo.addedEvents[0].deviceId)
        }

    @Test
    fun `recordReplacement updates device battery last replaced date`() =
        runTest {
            val repo = FakeDetailRepository()
            val device = createDevice(
                id = "device-1",
                name = "Test Device",
                batteryLastReplaced = Instant.DISTANT_PAST,
            )
            repo.setDevice(device)

            val viewModel = createViewModel(repo, "device-1")

            advanceUntilIdle()

            // Ensure we're in success state first
            viewModel.uiState.first { it is DeviceDetailUiState.Success }

            viewModel.recordReplacement()

            advanceUntilIdle()

            assertTrue(repo.updatedDevices.isNotEmpty())
            assertTrue(repo.updatedDevices[0].batteryLastReplaced > Instant.DISTANT_PAST)
        }

    private fun createDevice(
        id: String,
        name: String,
        typeId: String = "type-1",
        batteryLastReplaced: Instant = Instant.DISTANT_PAST,
        location: String? = null,
    ): Device =
        Device(
            id = id,
            name = name,
            typeId = typeId,
            batteryLastReplaced = batteryLastReplaced,
            lastUpdated = Instant.DISTANT_PAST,
            location = location,
        )

    private fun createViewModel(
        repo: FakeDetailRepository,
        deviceId: String,
    ): DeviceDetailViewModel =
        DeviceDetailViewModel(
            deviceId = deviceId,
            getDeviceDetailUseCase = GetDeviceDetailUseCase(repo),
            getDeviceTypesUseCase = GetDeviceTypesUseCase(repo),
            getBatteryEventsUseCase = GetBatteryEventsUseCase(repo),
            addBatteryEventUseCase = AddBatteryEventUseCase(repo),
            updateDeviceUseCase = UpdateDeviceUseCase(repo),
        )
}

@OptIn(ExperimentalTime::class)
private class FakeDetailRepository : DeviceRepository {
    private val deviceFlow = MutableStateFlow<Device?>(null)
    private val deviceTypesFlow = MutableStateFlow<List<DeviceType>>(emptyList())
    private val eventsMap = mutableMapOf<String, MutableStateFlow<List<BatteryEvent>>>()

    val addedEvents = mutableListOf<BatteryEvent>()
    val updatedDevices = mutableListOf<Device>()

    fun setDevice(device: Device?) {
        deviceFlow.value = device
    }

    fun setDeviceTypes(types: List<DeviceType>) {
        deviceTypesFlow.value = types
    }

    fun setEventsForDevice(
        deviceId: String,
        events: List<BatteryEvent>,
    ) {
        eventsMap.getOrPut(deviceId) { MutableStateFlow(emptyList()) }.value = events
    }

    override val syncStatus: StateFlow<SyncStatus> = MutableStateFlow(SyncStatus.Idle)

    override fun dismissSyncStatus() {}

    override fun getAllDevices(): Flow<List<Device>> = flowOf(listOfNotNull(deviceFlow.value))

    override fun getDeviceById(id: String): Flow<Device?> = deviceFlow

    override suspend fun addDevice(device: Device) {}

    override suspend fun updateDevice(device: Device) {
        updatedDevices.add(device)
        deviceFlow.value = device
    }

    override suspend fun deleteDevice(id: String) {}

    override fun getAllDeviceTypes(): Flow<List<DeviceType>> = deviceTypesFlow

    override fun getDeviceTypeById(id: String): Flow<DeviceType?> = flowOf(deviceTypesFlow.value.find { it.id == id })

    override suspend fun addDeviceType(type: DeviceType) {}

    override suspend fun updateDeviceType(type: DeviceType) {}

    override suspend fun deleteDeviceType(id: String) {}

    override fun getEventsForDevice(deviceId: String): Flow<List<BatteryEvent>> = eventsMap.getOrPut(deviceId) { MutableStateFlow(emptyList()) }

    override fun getAllEvents(): Flow<List<BatteryEvent>> = flowOf(eventsMap.values.flatMap { it.value })

    override fun getEventById(id: String): Flow<BatteryEvent?> = flowOf(eventsMap.values.flatMap { it.value }.find { it.id == id })

    override suspend fun addEvent(event: BatteryEvent) {
        addedEvents.add(event)
        val flow = eventsMap.getOrPut(event.deviceId) { MutableStateFlow(emptyList()) }
        flow.value = flow.value + event
    }

    override suspend fun updateEvent(event: BatteryEvent) {}

    override suspend fun deleteEvent(id: String) {}
}
