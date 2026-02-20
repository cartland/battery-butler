package com.chriscartland.batterybutler.viewmodel.history

import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationmodel.history.HistoryListUiState
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import com.chriscartland.batterybutler.usecase.GetBatteryEventsUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.GetDevicesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class HistoryListViewModelTest {
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
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo)

            val state = viewModel.uiState.value

            assertIs<HistoryListUiState.Loading>(state)
        }

    @Test
    fun `events with matching devices produce correct HistoryItemUiModel`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(id = "d1", name = "Kitchen Smoke", typeId = "type-1")
            val type = DeviceType(id = "type-1", name = "Smoke Detector")
            val event = TestDevices.createBatteryEvent(id = "e1", deviceId = "d1")
            repo.setDevices(listOf(device))
            repo.setDeviceTypes(listOf(type))
            repo.setEvents(listOf(event))

            val viewModel = createViewModel(repo)

            val state = viewModel.uiState.first { it is HistoryListUiState.Success }
            assertIs<HistoryListUiState.Success>(state)
            assertEquals(1, state.items.size)
            assertEquals("Kitchen Smoke", state.items[0].deviceName)
            assertEquals("Smoke Detector", state.items[0].deviceTypeName)
            assertEquals(event, state.items[0].event)
        }

    @Test
    fun `event with missing device shows Unknown Device`() =
        runTest {
            val repo = FakeDeviceRepository()
            val event = TestDevices.createBatteryEvent(id = "e1", deviceId = "nonexistent")
            repo.setEvents(listOf(event))

            val viewModel = createViewModel(repo)

            val state = viewModel.uiState.first { it is HistoryListUiState.Success }
            assertIs<HistoryListUiState.Success>(state)
            assertEquals(1, state.items.size)
            assertEquals("Unknown Device", state.items[0].deviceName)
            assertEquals("Unknown Type", state.items[0].deviceTypeName)
        }

    @Test
    fun `event with device but missing type shows Unknown Type`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(id = "d1", name = "My Device", typeId = "missing-type")
            val event = TestDevices.createBatteryEvent(id = "e1", deviceId = "d1")
            repo.setDevices(listOf(device))
            repo.setEvents(listOf(event))

            val viewModel = createViewModel(repo)

            val state = viewModel.uiState.first { it is HistoryListUiState.Success }
            assertIs<HistoryListUiState.Success>(state)
            assertEquals(1, state.items.size)
            assertEquals("My Device", state.items[0].deviceName)
            assertEquals("Unknown Type", state.items[0].deviceTypeName)
        }

    @Test
    fun `empty events produces Success with empty list`() =
        runTest {
            val repo = FakeDeviceRepository()
            // Emit empty lists for all flows (already default)

            val viewModel = createViewModel(repo)

            val state = viewModel.uiState.first { it is HistoryListUiState.Success }
            assertIs<HistoryListUiState.Success>(state)
            assertEquals(0, state.items.size)
        }

    @Test
    fun `device location flows through to HistoryItemUiModel`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(
                id = "d1",
                name = "Device",
                typeId = "type-1",
                location = "Kitchen",
            )
            val type = DeviceType(id = "type-1", name = "Smoke Detector")
            val event = TestDevices.createBatteryEvent(id = "e1", deviceId = "d1")
            repo.setDevices(listOf(device))
            repo.setDeviceTypes(listOf(type))
            repo.setEvents(listOf(event))

            val viewModel = createViewModel(repo)

            val state = viewModel.uiState.first { it is HistoryListUiState.Success }
            assertIs<HistoryListUiState.Success>(state)
            assertEquals("Kitchen", state.items[0].deviceLocation)
        }

    @Test
    fun `null device location flows through as null`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(
                id = "d1",
                name = "Device",
                typeId = "type-1",
                location = null,
            )
            val type = DeviceType(id = "type-1", name = "Type")
            val event = TestDevices.createBatteryEvent(id = "e1", deviceId = "d1")
            repo.setDevices(listOf(device))
            repo.setDeviceTypes(listOf(type))
            repo.setEvents(listOf(event))

            val viewModel = createViewModel(repo)

            val state = viewModel.uiState.first { it is HistoryListUiState.Success }
            assertIs<HistoryListUiState.Success>(state)
            assertNull(state.items[0].deviceLocation)
        }

    @Test
    fun `multiple events map to correct devices`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device1 = TestDevices.createDevice(id = "d1", name = "Device A", typeId = "type-1")
            val device2 = TestDevices.createDevice(id = "d2", name = "Device B", typeId = "type-2")
            val type1 = DeviceType(id = "type-1", name = "Type One")
            val type2 = DeviceType(id = "type-2", name = "Type Two")
            val event1 = TestDevices.createBatteryEvent(id = "e1", deviceId = "d1")
            val event2 = TestDevices.createBatteryEvent(id = "e2", deviceId = "d2")
            repo.setDevices(listOf(device1, device2))
            repo.setDeviceTypes(listOf(type1, type2))
            repo.setEvents(listOf(event1, event2))

            val viewModel = createViewModel(repo)

            val state = viewModel.uiState.first { it is HistoryListUiState.Success }
            assertIs<HistoryListUiState.Success>(state)
            assertEquals(2, state.items.size)
            assertEquals("Device A", state.items[0].deviceName)
            assertEquals("Type One", state.items[0].deviceTypeName)
            assertEquals("Device B", state.items[1].deviceName)
            assertEquals("Type Two", state.items[1].deviceTypeName)
        }

    @Test
    fun `events update reactively when new events are emitted`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(id = "d1", name = "Device", typeId = "type-1")
            val type = DeviceType(id = "type-1", name = "Type")
            repo.setDevices(listOf(device))
            repo.setDeviceTypes(listOf(type))

            val viewModel = createViewModel(repo)

            // Initially empty
            val emptyState = viewModel.uiState.first { it is HistoryListUiState.Success }
            assertIs<HistoryListUiState.Success>(emptyState)
            assertEquals(0, emptyState.items.size)

            // Add an event
            val event = TestDevices.createBatteryEvent(id = "e1", deviceId = "d1")
            repo.setEvents(listOf(event))

            val updatedState = viewModel.uiState.first {
                it is HistoryListUiState.Success && it.items.isNotEmpty()
            }
            assertIs<HistoryListUiState.Success>(updatedState)
            assertEquals(1, updatedState.items.size)
            assertEquals("Device", updatedState.items[0].deviceName)
        }

    private fun createViewModel(repo: FakeDeviceRepository): HistoryListViewModel =
        HistoryListViewModel(
            getBatteryEventsUseCase = GetBatteryEventsUseCase(repo),
            getDevicesUseCase = GetDevicesUseCase(repo),
            getDeviceTypesUseCase = GetDeviceTypesUseCase(repo),
        )
}
