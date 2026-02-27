package com.chriscartland.batterybutler.viewmodel.adddevice

import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.usecase.AddDeviceUseCase
import com.chriscartland.batterybutler.usecase.BatchAddDevicesUseCase
import com.chriscartland.batterybutler.usecase.FindOrCreateDeviceTypeUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CrashProofViewModelTest {
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
    fun `ViewModel initialization does not crash when upstream flow throws exception`() =
        runTest {
            // Create an intentionally broken repository that throws on flow collection
            // like an uninitialized SQLite driver on iOS would.
            val fakeRepo = FakeDeviceRepository()
            val throwingRepo = object : DeviceRepository by fakeRepo {
                override fun getAllDeviceTypes(): Flow<List<DeviceType>> =
                    flow {
                        throw RuntimeException("Simulated SQLite driver failure on iOS")
                    }
            }

            val viewModel = AddDeviceViewModel(
                addDeviceUseCase = AddDeviceUseCase(throwingRepo),
                getDeviceTypesUseCase = GetDeviceTypesUseCase(throwingRepo),
                batchAddDevicesUseCase = BatchAddDevicesUseCase(
                    FakeAiEngine(),
                    AddDeviceUseCase(throwingRepo),
                    FindOrCreateDeviceTypeUseCase(throwingRepo),
                ),
                featureFlagProvider = FakeFeatureFlagProvider(),
            )

            // Read the property to trigger the stateIn flow collection
            val value = viewModel.deviceTypes.value

            // Advance coroutines, which will execute the upstream flow and throw the exception
            testDispatcher.scheduler.advanceUntilIdle()

            // If 'safeStateIn' is working, the exception is caught and printed, and this test passes.
            // If 'stateIn' was used without try-catch, the unhandled exception propagates
            // and fails this test (or crashes the iOS app in production).
            assertTrue(value.isEmpty())
        }
}
