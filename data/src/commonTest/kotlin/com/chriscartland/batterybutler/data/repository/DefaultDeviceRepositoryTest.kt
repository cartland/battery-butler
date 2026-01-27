package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.datalocal.LocalDataSource
import com.chriscartland.batterybutler.datanetwork.RemoteDataSource
import com.chriscartland.batterybutler.datanetwork.RemoteDataSourceState
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
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
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class DefaultDeviceRepositoryTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Device Tests
    @Test
    fun `addDevice saves to local and pushes to remote`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val repo = DefaultDeviceRepository(local, remote, this)
            val device = createDevice(id = "1", name = "Test Device")

            repo.addDevice(device)
            advanceUntilIdle()

            assertEquals(1, local.devices.size)
            assertEquals(device, local.devices[0])
            assertEquals(1, remote.pushedUpdates.size)
            assertEquals(device, remote.pushedUpdates[0].devices.firstOrNull())
        }

    @Test
    fun `updateDevice updates local and pushes to remote`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val repo = DefaultDeviceRepository(local, remote, this)
            val device = createDevice(id = "1", name = "Updated Device")

            repo.updateDevice(device)
            advanceUntilIdle()

            assertTrue(local.updatedDevices.contains(device))
            assertEquals(device, remote.pushedUpdates[0].devices.firstOrNull())
        }

    @Test
    fun `deleteDevice removes from local`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val repo = DefaultDeviceRepository(local, remote, this)

            repo.deleteDevice("device-1")
            advanceUntilIdle()

            assertTrue(local.deletedDeviceIds.contains("device-1"))
        }

    // Device Type Tests
    @Test
    fun `addDeviceType saves to local and pushes to remote`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val repo = DefaultDeviceRepository(local, remote, this)
            val type = DeviceType(id = "type-1", name = "Smoke Detector")

            repo.addDeviceType(type)
            advanceUntilIdle()

            assertEquals(1, local.deviceTypes.size)
            assertEquals(type, local.deviceTypes[0])
            assertEquals(type, remote.pushedUpdates[0].deviceTypes.firstOrNull())
        }

    @Test
    fun `updateDeviceType updates local and pushes to remote`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val repo = DefaultDeviceRepository(local, remote, this)
            val type = DeviceType(id = "type-1", name = "Updated Type")

            repo.updateDeviceType(type)
            advanceUntilIdle()

            assertTrue(local.updatedDeviceTypes.contains(type))
            assertEquals(type, remote.pushedUpdates[0].deviceTypes.firstOrNull())
        }

    @Test
    fun `deleteDeviceType removes from local`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val repo = DefaultDeviceRepository(local, remote, this)

            repo.deleteDeviceType("type-1")
            advanceUntilIdle()

            assertTrue(local.deletedDeviceTypeIds.contains("type-1"))
        }

    // Battery Event Tests
    @Test
    fun `addEvent saves to local and pushes to remote`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val repo = DefaultDeviceRepository(local, remote, this)
            val event = createBatteryEvent(id = "event-1", deviceId = "device-1")

            repo.addEvent(event)
            advanceUntilIdle()

            assertEquals(1, local.events.size)
            assertEquals(event, local.events[0])
            assertEquals(event, remote.pushedUpdates[0].events.firstOrNull())
        }

    @Test
    fun `updateEvent updates local and pushes to remote`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val repo = DefaultDeviceRepository(local, remote, this)
            val event = createBatteryEvent(id = "event-1", deviceId = "device-1")

            repo.updateEvent(event)
            advanceUntilIdle()

            assertTrue(local.updatedEvents.contains(event))
            assertEquals(event, remote.pushedUpdates[0].events.firstOrNull())
        }

    @Test
    fun `deleteEvent removes from local`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val repo = DefaultDeviceRepository(local, remote, this)

            repo.deleteEvent("event-1")
            advanceUntilIdle()

            assertTrue(local.deletedEventIds.contains("event-1"))
        }

    // Flow delegation tests
    @Test
    fun `getAllDevices returns flow from local data source`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val device = createDevice(id = "1", name = "Test")
            local.setDevicesForFlow(listOf(device))
            val repo = DefaultDeviceRepository(local, remote, this)

            // The repository delegates to local
            val flow = repo.getAllDevices()
            assertEquals(local.getAllDevices(), flow)
        }

    @Test
    fun `getAllDeviceTypes returns flow from local data source`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val type = DeviceType(id = "type-1", name = "Test")
            local.setDeviceTypesForFlow(listOf(type))
            val repo = DefaultDeviceRepository(local, remote, this)

            val flow = repo.getAllDeviceTypes()
            assertEquals(local.getAllDeviceTypes(), flow)
        }

    // Remote push failure test
    @Test
    fun `pushUpdate handles remote failure gracefully`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            remote.shouldFail = true
            val repo = DefaultDeviceRepository(local, remote, this)
            val device = createDevice(id = "1", name = "Test Device")

            repo.addDevice(device)
            advanceUntilIdle()

            // Device should still be saved locally
            assertEquals(1, local.devices.size)
            // Remote push was attempted
            assertEquals(1, remote.pushedUpdates.size)
        }

    private fun createDevice(
        id: String,
        name: String,
        typeId: String = "type-1",
    ): Device =
        Device(
            id = id,
            name = name,
            typeId = typeId,
            batteryLastReplaced = Instant.DISTANT_PAST,
            lastUpdated = Instant.DISTANT_PAST,
        )

    private fun createBatteryEvent(
        id: String,
        deviceId: String,
    ): BatteryEvent =
        BatteryEvent(
            id = id,
            deviceId = deviceId,
            date = Instant.DISTANT_PAST,
        )
}

class FakeLocalDataSource : LocalDataSource {
    val devices = mutableListOf<Device>()
    val updatedDevices = mutableListOf<Device>()
    val deletedDeviceIds = mutableListOf<String>()
    val deviceTypes = mutableListOf<DeviceType>()
    val updatedDeviceTypes = mutableListOf<DeviceType>()
    val deletedDeviceTypeIds = mutableListOf<String>()
    val events = mutableListOf<BatteryEvent>()
    val updatedEvents = mutableListOf<BatteryEvent>()
    val deletedEventIds = mutableListOf<String>()

    private var devicesFlow: Flow<List<Device>> = emptyFlow()
    private var deviceTypesFlow: Flow<List<DeviceType>> = emptyFlow()

    fun setDevicesForFlow(devices: List<Device>) {
        devicesFlow = flowOf(devices)
    }

    fun setDeviceTypesForFlow(types: List<DeviceType>) {
        deviceTypesFlow = flowOf(types)
    }

    override fun getAllDevices(): Flow<List<Device>> = devicesFlow

    override fun getDeviceById(id: String): Flow<Device?> = emptyFlow()

    override suspend fun addDevice(device: Device) {
        devices.add(device)
    }

    override suspend fun updateDevice(device: Device) {
        updatedDevices.add(device)
    }

    override suspend fun deleteDevice(id: String) {
        deletedDeviceIds.add(id)
    }

    override fun getAllDeviceTypes(): Flow<List<DeviceType>> = deviceTypesFlow

    override fun getDeviceTypeById(id: String): Flow<DeviceType?> = emptyFlow()

    override suspend fun addDeviceType(type: DeviceType) {
        deviceTypes.add(type)
    }

    override suspend fun updateDeviceType(type: DeviceType) {
        updatedDeviceTypes.add(type)
    }

    override suspend fun deleteDeviceType(id: String) {
        deletedDeviceTypeIds.add(id)
    }

    override fun getEventsForDevice(deviceId: String): Flow<List<BatteryEvent>> = emptyFlow()

    override fun getAllEvents(): Flow<List<BatteryEvent>> = emptyFlow()

    override fun getEventById(id: String): Flow<BatteryEvent?> = emptyFlow()

    override suspend fun addEvent(event: BatteryEvent) {
        events.add(event)
    }

    override suspend fun updateEvent(event: BatteryEvent) {
        updatedEvents.add(event)
    }

    override suspend fun deleteEvent(id: String) {
        deletedEventIds.add(id)
    }
}

class FakeRemoteDataSource : RemoteDataSource {
    val pushedUpdates = mutableListOf<RemoteUpdate>()
    var shouldFail = false

    override val state: StateFlow<RemoteDataSourceState> = MutableStateFlow(RemoteDataSourceState.NotStarted)

    override fun subscribe(): Flow<RemoteUpdate> = emptyFlow()

    override suspend fun push(update: RemoteUpdate): Boolean {
        pushedUpdates.add(update)
        return !shouldFail
    }
}
