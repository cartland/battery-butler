package com.chriscartland.batterybutler.viewmodel.devicetypes

import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeDetailScreenState
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import com.chriscartland.batterybutler.usecase.GetDeviceTypeDetailUseCase
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

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceTypeDetailViewModelTest {
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
            val viewModel = createViewModel(repo, "type-1")

            assertEquals(DeviceTypeDetailScreenState.Loading, viewModel.uiState.value)
        }

    @Test
    fun `type not found emits NotFound state`() =
        runTest {
            val repo = FakeDeviceRepository()

            val viewModel = createViewModel(repo, "nonexistent-type")

            val state = viewModel.uiState.first { it is DeviceTypeDetailScreenState.NotFound }
            assertIs<DeviceTypeDetailScreenState.NotFound>(state)
        }

    @Test
    fun `type found with devices emits Success state`() =
        runTest {
            val repo = FakeDeviceRepository()
            val deviceType = TestDevices.createDeviceType(id = "type-1", name = "Smoke Detector")
            val device = TestDevices.createDevice(id = "device-1", name = "Kitchen Detector", typeId = "type-1")
            val otherDevice = TestDevices.createDevice(id = "device-2", name = "Other Device", typeId = "type-2")
            repo.setDeviceTypes(listOf(deviceType))
            repo.setDevices(listOf(device, otherDevice))

            val viewModel = createViewModel(repo, "type-1")

            val state = viewModel.uiState.first { it is DeviceTypeDetailScreenState.Success }
            assertIs<DeviceTypeDetailScreenState.Success>(state)
            assertEquals("Smoke Detector", state.deviceType.name)
            assertEquals(1, state.devices.size)
            assertEquals("Kitchen Detector", state.devices.first().name)
        }

    private fun createViewModel(
        repo: FakeDeviceRepository,
        typeId: String,
    ): DeviceTypeDetailViewModel =
        DeviceTypeDetailViewModel(
            typeId = typeId,
            getDeviceTypeDetailUseCase = GetDeviceTypeDetailUseCase(repo),
            getDevicesUseCase = GetDevicesUseCase(repo),
        )
}
