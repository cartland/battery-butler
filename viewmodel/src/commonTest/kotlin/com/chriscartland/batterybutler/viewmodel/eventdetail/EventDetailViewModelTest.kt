package com.chriscartland.batterybutler.viewmodel.eventdetail

import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationmodel.eventdetail.EventDetailScreenState
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import com.chriscartland.batterybutler.usecase.DeleteBatteryEventUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceDetailUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.GetEventDetailUseCase
import com.chriscartland.batterybutler.usecase.UpdateDeviceLastReplacedUseCase
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

@OptIn(ExperimentalCoroutinesApi::class)
class EventDetailViewModelTest {
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
            val viewModel = createViewModel(repo, "event-1")

            assertEquals(EventDetailScreenState.Loading, viewModel.uiState.value)
        }

    @Test
    fun `event not found emits NotFound state`() =
        runTest {
            val repo = FakeDeviceRepository()

            val viewModel = createViewModel(repo, "nonexistent-event")

            val state = viewModel.uiState.first { it is EventDetailScreenState.NotFound }
            assertIs<EventDetailScreenState.NotFound>(state)
        }

    @Test
    fun `event found with device and type emits Success`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(id = "device-1", name = "Smoke Detector", typeId = "type-1")
            val type = DeviceType(id = "type-1", name = "Smoke Detector Type", batteryType = "9V")
            val event = TestDevices.createBatteryEvent(id = "event-1", deviceId = "device-1", batteryType = "9V")
            repo.setDevices(listOf(device))
            repo.setDeviceTypes(listOf(type))
            repo.setEvents(listOf(event))

            val viewModel = createViewModel(repo, "event-1")

            val state = viewModel.uiState.first { it is EventDetailScreenState.Success }
            assertIs<EventDetailScreenState.Success>(state)
            assertEquals("event-1", state.event.id)
            assertEquals("Smoke Detector", state.device?.name)
            assertEquals("Smoke Detector Type", state.deviceType?.name)
        }

    @Test
    fun `event found with device but no matching type has null deviceType`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(id = "device-1", name = "Test Device", typeId = "type-unknown")
            val event = TestDevices.createBatteryEvent(id = "event-1", deviceId = "device-1")
            repo.setDevices(listOf(device))
            repo.setEvents(listOf(event))

            val viewModel = createViewModel(repo, "event-1")

            val state = viewModel.uiState.first { it is EventDetailScreenState.Success }
            assertIs<EventDetailScreenState.Success>(state)
            assertNull(state.deviceType)
        }

    private fun createViewModel(
        repo: FakeDeviceRepository,
        eventId: String,
    ): EventDetailViewModel =
        EventDetailViewModel(
            eventId = eventId,
            getEventDetailUseCase = GetEventDetailUseCase(repo),
            getDeviceDetailUseCase = GetDeviceDetailUseCase(repo),
            getDeviceTypesUseCase = GetDeviceTypesUseCase(repo),
            deleteBatteryEventUseCase = DeleteBatteryEventUseCase(repo, UpdateDeviceLastReplacedUseCase(repo)),
        )
}
