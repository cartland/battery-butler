package com.chriscartland.batterybutler.viewmodel.home

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.SyncStatus
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.presentationmodel.home.GroupOption
import com.chriscartland.batterybutler.presentationmodel.home.SortOption
import com.chriscartland.batterybutler.usecase.DismissSyncStatusUseCase
import com.chriscartland.batterybutler.usecase.ExportDataUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.GetDevicesUseCase
import com.chriscartland.batterybutler.usecase.GetSyncStatusUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class HomeViewModelTest {
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
    fun `initial state is empty`() =
        runTest {
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo)

            val state = viewModel.uiState.first()

            assertEquals(
                0,
                state.groupedDevices.values
                    .flatten()
                    .size,
            )
        }

    @Test
    fun `loads devices and types`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = createDevice(id = "1", name = "Test Device", typeId = "type-1")
            val type = DeviceType(id = "type-1", name = "Test Type")
            repo.setDevices(listOf(device))
            repo.setDeviceTypes(listOf(type))

            val viewModel = createViewModel(repo)

            val state = viewModel.uiState.first {
                it.groupedDevices.values
                    .flatten()
                    .isNotEmpty()
            }

            assertEquals(
                1,
                state.groupedDevices.values
                    .flatten()
                    .size,
            )
            assertEquals(device, state.groupedDevices.values.flatten()[0])
            assertEquals("Test Type", state.deviceTypes["type-1"]?.name)
        }

    @Test
    fun `devices are sorted by name by default`() =
        runTest {
            val repo = FakeDeviceRepository()
            val deviceA = createDevice(id = "1", name = "Alpha")
            val deviceB = createDevice(id = "2", name = "Zulu")
            val deviceC = createDevice(id = "3", name = "Bravo")
            repo.setDevices(listOf(deviceB, deviceA, deviceC))

            val viewModel = createViewModel(repo)

            val state = viewModel.uiState.first {
                it.groupedDevices.values
                    .flatten()
                    .size == 3
            }
            val devices = state.groupedDevices.values.flatten()

            assertEquals("Alpha", devices[0].name)
            assertEquals("Bravo", devices[1].name)
            assertEquals("Zulu", devices[2].name)
        }

    @Test
    fun `onSortOptionSelected changes sort option`() =
        runTest {
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo)

            viewModel.onSortOptionSelected(SortOption.LOCATION)

            val state = viewModel.uiState.first { it.sortOption == SortOption.LOCATION }
            assertEquals(SortOption.LOCATION, state.sortOption)
        }

    @Test
    fun `onGroupOptionSelected changes group option`() =
        runTest {
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo)

            viewModel.onGroupOptionSelected(GroupOption.TYPE)

            val state = viewModel.uiState.first { it.groupOption == GroupOption.TYPE }
            assertEquals(GroupOption.TYPE, state.groupOption)
        }

    @Test
    fun `toggleSortDirection inverts sort direction`() =
        runTest {
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo)

            val initialState = viewModel.uiState.first()
            assertTrue(initialState.isSortAscending)

            viewModel.toggleSortDirection()

            val updatedState = viewModel.uiState.first { !it.isSortAscending }
            assertFalse(updatedState.isSortAscending)
        }

    @Test
    fun `toggleGroupDirection inverts group direction`() =
        runTest {
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo)

            val initialState = viewModel.uiState.first()
            assertTrue(initialState.isGroupAscending)

            viewModel.toggleGroupDirection()

            val updatedState = viewModel.uiState.first { !it.isGroupAscending }
            assertFalse(updatedState.isGroupAscending)
        }

    @Test
    fun `grouping by type groups devices correctly`() =
        runTest {
            val repo = FakeDeviceRepository()
            val type1 = DeviceType(id = "type-1", name = "Smoke Detector")
            val type2 = DeviceType(id = "type-2", name = "Remote")
            val device1 = createDevice(id = "1", name = "Kitchen Smoke", typeId = "type-1")
            val device2 = createDevice(id = "2", name = "Living Room Smoke", typeId = "type-1")
            val device3 = createDevice(id = "3", name = "TV Remote", typeId = "type-2")
            repo.setDeviceTypes(listOf(type1, type2))
            repo.setDevices(listOf(device1, device2, device3))

            val viewModel = createViewModel(repo)
            viewModel.onGroupOptionSelected(GroupOption.TYPE)

            val state = viewModel.uiState.first {
                it.groupOption == GroupOption.TYPE &&
                    it.groupedDevices.values
                        .flatten()
                        .size == 3
            }

            assertTrue(state.groupedDevices.containsKey("Smoke Detector"))
            assertTrue(state.groupedDevices.containsKey("Remote"))
            assertEquals(2, state.groupedDevices["Smoke Detector"]?.size)
            assertEquals(1, state.groupedDevices["Remote"]?.size)
        }

    @Test
    fun `grouping by location groups devices correctly`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device1 = createDevice(id = "1", name = "Device 1", location = "Kitchen")
            val device2 = createDevice(id = "2", name = "Device 2", location = "Kitchen")
            val device3 = createDevice(id = "3", name = "Device 3", location = "Bedroom")
            repo.setDevices(listOf(device1, device2, device3))

            val viewModel = createViewModel(repo)
            viewModel.onGroupOptionSelected(GroupOption.LOCATION)

            val state = viewModel.uiState.first {
                it.groupOption == GroupOption.LOCATION &&
                    it.groupedDevices.values
                        .flatten()
                        .size == 3
            }

            assertTrue(state.groupedDevices.containsKey("Kitchen"))
            assertTrue(state.groupedDevices.containsKey("Bedroom"))
            assertEquals(2, state.groupedDevices["Kitchen"]?.size)
            assertEquals(1, state.groupedDevices["Bedroom"]?.size)
        }

    private fun createDevice(
        id: String,
        name: String,
        typeId: String = "type-1",
        location: String? = null,
        batteryLastReplaced: Instant = Instant.DISTANT_PAST,
    ): Device =
        Device(
            id = id,
            name = name,
            typeId = typeId,
            batteryLastReplaced = batteryLastReplaced,
            lastUpdated = Instant.DISTANT_PAST,
            location = location,
        )

    private fun createViewModel(repo: DeviceRepository): HomeViewModel =
        HomeViewModel(
            getDevicesUseCase = GetDevicesUseCase(repo),
            getDeviceTypesUseCase = GetDeviceTypesUseCase(repo),
            exportDataUseCase = ExportDataUseCase(repo),
            getSyncStatusUseCase = GetSyncStatusUseCase(repo),
            dismissSyncStatusUseCase = DismissSyncStatusUseCase(repo),
        )
}

class FakeDeviceRepository : DeviceRepository {
    private val devicesFlow = MutableStateFlow<List<Device>>(emptyList())
    private val deviceTypesFlow = MutableStateFlow<List<DeviceType>>(emptyList())
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)

    fun setDevices(devices: List<Device>) {
        devicesFlow.value = devices
    }

    fun setDeviceTypes(types: List<DeviceType>) {
        deviceTypesFlow.value = types
    }

    override val syncStatus: StateFlow<SyncStatus> = _syncStatus

    override fun dismissSyncStatus() {
        _syncStatus.value = SyncStatus.Idle
    }

    override fun getAllDevices(): Flow<List<Device>> = devicesFlow

    override fun getDeviceById(id: String): Flow<Device?> = emptyFlow()

    override suspend fun addDevice(device: Device) {}

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
