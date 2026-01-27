package com.chriscartland.batterybutler.viewmodel.home

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.SyncStatus
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
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
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
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
            val device = Device(
                id = "1",
                name = "Test Device",
                typeId = "type-1",
                batteryLastReplaced = Instant.DISTANT_PAST,
                lastUpdated = Instant.DISTANT_PAST,
            )
            val type = DeviceType(id = "type-1", name = "Test Type")
            repo.setDevices(listOf(device))
            repo.setDeviceTypes(listOf(type))

            val viewModel = createViewModel(repo)

            // Trigger collection (stateIn is lazy/WhileSubscribed)
            // usage of runTest with StandardTestDispatcher might require advanceUntilIdle if we used launch
            // but stateIn initial value + flow updates should work.

            // We need to collect the flow to get updates.
            // Combining flows in ViewModel happens immediately? No, it's a StateFlow.
            // The repository emits flowOf(devices).

            // With StandardTestDispatcher, we might need to yield.

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

    private fun createViewModel(repo: DeviceRepository): HomeViewModel =
        HomeViewModel(
            getDevicesUseCase = GetDevicesUseCase(repo),
            getDeviceTypesUseCase = GetDeviceTypesUseCase(repo),
            exportDataUseCase = ExportDataUseCase(repo),
            getSyncStatusUseCase = GetSyncStatusUseCase(repo),
        )
}

class FakeDeviceRepository : DeviceRepository {
    private val devicesFlow = MutableStateFlow<List<Device>>(emptyList())
    private val deviceTypesFlow = MutableStateFlow<List<DeviceType>>(emptyList())

    fun setDevices(devices: List<Device>) {
        devicesFlow.value = devices
    }

    fun setDeviceTypes(types: List<DeviceType>) {
        deviceTypesFlow.value = types
    }

    override val syncStatus: StateFlow<SyncStatus> = MutableStateFlow(SyncStatus.Idle)

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
