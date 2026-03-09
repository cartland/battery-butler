package com.chriscartland.batterybutler.viewmodel.history

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.presentationmodel.history.HistoryListUiState
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.usecase.GetBatteryEventsUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.GetDevicesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
class CrashProofHistoryListViewModelTest {
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
    fun `uiState transitions to Error when getAllEvents flow throws`() =
        runTest {
            val fakeRepo = FakeDeviceRepository()
            val throwingRepo = object : DeviceRepository by fakeRepo {
                override fun getAllEvents(): Flow<List<BatteryEvent>> =
                    flow {
                        throw RuntimeException("Simulated getAllEvents failure")
                    }
            }

            val viewModel = createViewModel(throwingRepo)

            // Trigger the stateIn flow collection
            viewModel.uiState.value

            // Advance coroutines so the upstream flow throws
            testDispatcher.scheduler.advanceUntilIdle()

            // safeStateIn catches the exception and emits an Error state
            val finalState = viewModel.uiState.value
            assertIs<HistoryListUiState.Error>(finalState)
            assertEquals("Simulated getAllEvents failure", finalState.message)
        }

    @Test
    fun `uiState transitions to Error when getAllDevices flow throws`() =
        runTest {
            val fakeRepo = FakeDeviceRepository()
            val throwingRepo = object : DeviceRepository by fakeRepo {
                override fun getAllDevices(): Flow<List<Device>> =
                    flow {
                        throw RuntimeException("Simulated getAllDevices failure")
                    }
            }

            val viewModel = createViewModel(throwingRepo)

            // Trigger the stateIn flow collection
            viewModel.uiState.value

            // Advance coroutines so the upstream flow throws
            testDispatcher.scheduler.advanceUntilIdle()

            // safeStateIn catches the exception and emits an Error state
            val finalState = viewModel.uiState.value
            assertIs<HistoryListUiState.Error>(finalState)
            assertEquals("Simulated getAllDevices failure", finalState.message)
        }

    @Test
    fun `uiState transitions to Error when getAllDeviceTypes flow throws`() =
        runTest {
            val fakeRepo = FakeDeviceRepository()
            val throwingRepo = object : DeviceRepository by fakeRepo {
                override fun getAllDeviceTypes(): Flow<List<DeviceType>> =
                    flow {
                        throw RuntimeException("Simulated getAllDeviceTypes failure")
                    }
            }

            val viewModel = createViewModel(throwingRepo)

            // Trigger the stateIn flow collection
            viewModel.uiState.value

            // Advance coroutines so the upstream flow throws
            testDispatcher.scheduler.advanceUntilIdle()

            // safeStateIn catches the exception and emits an Error state
            val finalState = viewModel.uiState.value
            assertIs<HistoryListUiState.Error>(finalState)
            assertEquals("Simulated getAllDeviceTypes failure", finalState.message)
        }

    private fun createViewModel(repo: DeviceRepository): HistoryListViewModel =
        HistoryListViewModel(
            getBatteryEventsUseCase = GetBatteryEventsUseCase(repo),
            getDevicesUseCase = GetDevicesUseCase(repo),
            getDeviceTypesUseCase = GetDeviceTypesUseCase(repo),
        )
}
