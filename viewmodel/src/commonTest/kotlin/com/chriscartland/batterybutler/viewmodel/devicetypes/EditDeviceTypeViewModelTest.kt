package com.chriscartland.batterybutler.viewmodel.devicetypes

import com.chriscartland.batterybutler.domain.model.DeviceTypeInput
import com.chriscartland.batterybutler.presentationmodel.devicetypes.EditDeviceTypeScreenState
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import com.chriscartland.batterybutler.usecase.DeleteDeviceTypeUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.UpdateDeviceTypeUseCase
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EditDeviceTypeViewModelTest {
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

            assertEquals(EditDeviceTypeScreenState.Loading, viewModel.uiState.value)
        }

    @Test
    fun `loads device type by ID on init`() =
        runTest {
            val repo = FakeDeviceRepository()
            val type = TestDevices.createDeviceType(id = "type-1", name = "Smoke Detector", batteryType = "9V")
            repo.setDeviceTypes(listOf(type))

            val viewModel = createViewModel(repo, "type-1")

            val state = viewModel.uiState.first { it is EditDeviceTypeScreenState.Success }
            assertIs<EditDeviceTypeScreenState.Success>(state)
            assertEquals("Smoke Detector", state.deviceType.name)
            assertEquals("9V", state.deviceType.batteryType)
        }

    @Test
    fun `shows NotFound state for missing type ID`() =
        runTest {
            val repo = FakeDeviceRepository()

            val viewModel = createViewModel(repo, "nonexistent-id")

            val state = viewModel.uiState.first { it is EditDeviceTypeScreenState.NotFound }
            assertIs<EditDeviceTypeScreenState.NotFound>(state)
        }

    @Test
    fun `updateDeviceType calls use case with updated fields`() =
        runTest {
            val repo = FakeDeviceRepository()
            val type = TestDevices.createDeviceType(id = "type-1", name = "Old Name", batteryType = "AA")
            repo.setDeviceTypes(listOf(type))

            val viewModel = createViewModel(repo, "type-1")
            viewModel.uiState.first { it is EditDeviceTypeScreenState.Success }

            val input = DeviceTypeInput(
                name = "New Name",
                defaultIcon = "lightbulb",
                batteryType = "AAA",
                batteryQuantity = 3,
            )
            viewModel.updateDeviceType(input)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("New Name", repo.deviceTypes[0].name)
            assertEquals("AAA", repo.deviceTypes[0].batteryType)
            assertEquals(3, repo.deviceTypes[0].batteryQuantity)
            assertEquals("lightbulb", repo.deviceTypes[0].defaultIcon)
        }

    @Test
    fun `deleteDeviceType removes type from repository`() =
        runTest {
            val repo = FakeDeviceRepository()
            val type = TestDevices.createDeviceType(id = "type-1", name = "Test Type")
            repo.setDeviceTypes(listOf(type))

            val viewModel = createViewModel(repo, "type-1")

            viewModel.deleteDeviceType()
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(repo.deviceTypes.isEmpty())
        }

    private fun createViewModel(
        repo: FakeDeviceRepository,
        typeId: String,
    ): EditDeviceTypeViewModel =
        EditDeviceTypeViewModel(
            typeId = typeId,
            getDeviceTypesUseCase = GetDeviceTypesUseCase(repo),
            updateDeviceTypeUseCase = UpdateDeviceTypeUseCase(repo),
            deleteDeviceTypeUseCase = DeleteDeviceTypeUseCase(repo),
        )
}
