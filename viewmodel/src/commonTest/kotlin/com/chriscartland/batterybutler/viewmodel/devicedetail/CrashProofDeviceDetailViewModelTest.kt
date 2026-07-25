package com.chriscartland.batterybutler.viewmodel.devicedetail

import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import com.chriscartland.batterybutler.domain.repository.DeviceImageRepository
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.presentationmodel.devicedetail.DeviceDetailScreenState
import com.chriscartland.batterybutler.testcommon.FakeDeviceImageRepository
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import com.chriscartland.batterybutler.usecase.AddBatteryEventUseCase
import com.chriscartland.batterybutler.usecase.GetBatteryEventsUseCase
import com.chriscartland.batterybutler.usecase.GetCachedDeviceImageUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceDetailUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.UpdateDeviceLastReplacedUseCase
import com.chriscartland.batterybutler.usecase.UpdateDeviceUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Regression tests for the production wedge seen on android/57: a transient Room/SQLite
 * exception in the Device Details state chain (detail queries race the sync loop's full
 * snapshot upsert every poll) terminated the flow inside `safeStateIn`, leaving the screen
 * permanently stuck at Loading with zero network requests. These tests pin that an upstream
 * throw now surfaces as [DeviceDetailScreenState.Error] and that `retry()` recovers.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CrashProofDeviceDetailViewModelTest {
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
    fun `uiState transitions to Error when getDeviceById flow throws`() =
        runTest {
            val fakeRepo = FakeDeviceRepository()
            val throwingRepo = object : DeviceRepository by fakeRepo {
                override fun getDeviceById(id: String): Flow<Device?> =
                    flow {
                        throw RuntimeException("Simulated Room failure during sync upsert")
                    }
            }

            val viewModel = createViewModel(throwingRepo)

            // Actively subscribe to trigger the stateIn flow collection
            val job = launch { viewModel.uiState.collect {} }
            testDispatcher.scheduler.advanceUntilIdle()

            // The catch emits an Error state instead of terminating the flow (pre-fix: the
            // flow terminated and uiState sat at Loading forever).
            val finalState = viewModel.uiState.value
            assertIs<DeviceDetailScreenState.Error>(finalState)
            assertEquals("Simulated Room failure during sync upsert", finalState.message)

            job.cancel()
        }

    @Test
    fun `uiState transitions to Error when the cached-image flow throws mid-chain`() =
        runTest {
            val fakeRepo = FakeDeviceRepository()
            fakeRepo.setDevices(
                listOf(TestDevices.createDevice(id = "device-1").copy(imageEtag = "etag-1")),
            )
            val fakeImageRepo = FakeDeviceImageRepository()
            val throwingImageRepo = object : DeviceImageRepository by fakeImageRepo {
                override fun observeCachedImage(imageEtag: String): Flow<DeviceImageBytes?> =
                    flow {
                        throw RuntimeException("Simulated image cache read failure")
                    }
            }

            val viewModel = createViewModel(fakeRepo, throwingImageRepo)

            val job = launch { viewModel.uiState.collect {} }
            testDispatcher.scheduler.advanceUntilIdle()

            val finalState = viewModel.uiState.value
            assertIs<DeviceDetailScreenState.Error>(finalState)
            assertEquals("Simulated image cache read failure", finalState.message)

            job.cancel()
        }

    @Test
    fun `retry re-subscribes and recovers to Success after a transient failure`() =
        runTest {
            val fakeRepo = FakeDeviceRepository()
            fakeRepo.setDevices(listOf(TestDevices.createDevice(id = "device-1", name = "Smoke Detector")))
            var failuresRemaining = 1
            val flakyRepo = object : DeviceRepository by fakeRepo {
                override fun getDeviceById(id: String): Flow<Device?> =
                    flow {
                        if (failuresRemaining > 0) {
                            failuresRemaining--
                            throw RuntimeException("Transient DB failure")
                        }
                        emitAll(fakeRepo.getDeviceById(id))
                    }
            }

            val viewModel = createViewModel(flakyRepo)

            val job = launch { viewModel.uiState.collect {} }
            testDispatcher.scheduler.advanceUntilIdle()
            assertIs<DeviceDetailScreenState.Error>(viewModel.uiState.value)

            viewModel.retry()
            testDispatcher.scheduler.advanceUntilIdle()

            val recovered = viewModel.uiState.value
            assertIs<DeviceDetailScreenState.Success>(recovered)
            assertEquals("device-1", recovered.device.id)
            assertEquals("Smoke Detector", recovered.device.name)

            job.cancel()
        }

    private fun createViewModel(
        repo: DeviceRepository,
        imageRepo: DeviceImageRepository = FakeDeviceImageRepository(),
        deviceId: String = "device-1",
    ): DeviceDetailViewModel =
        DeviceDetailViewModel(
            deviceId = deviceId,
            getDeviceDetailUseCase = GetDeviceDetailUseCase(repo),
            getDeviceTypesUseCase = GetDeviceTypesUseCase(repo),
            getBatteryEventsUseCase = GetBatteryEventsUseCase(repo),
            addBatteryEventUseCase = AddBatteryEventUseCase(repo, UpdateDeviceLastReplacedUseCase(repo)),
            updateDeviceUseCase = UpdateDeviceUseCase(repo),
            getCachedDeviceImageUseCase = GetCachedDeviceImageUseCase(imageRepo),
        )
}
