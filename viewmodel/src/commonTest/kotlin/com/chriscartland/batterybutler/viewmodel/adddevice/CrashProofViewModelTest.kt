package com.chriscartland.batterybutler.viewmodel.adddevice

import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceInput
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.Result
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

            // safeStateIn catches the exception and falls back to emptyList()
            assertTrue(value.isEmpty())
        }

    @Test
    fun `addDevice sets actionError when repo returns error`() =
        runTest {
            val fakeRepo = FakeDeviceRepository()
            val errorRepo = object : DeviceRepository by fakeRepo {
                override suspend fun addDevice(device: Device): Result<Unit, DataError> = Result.Error(DataError.Database.WriteFailed(message = "Simulated addDevice failure"))
            }

            val viewModel = AddDeviceViewModel(
                addDeviceUseCase = AddDeviceUseCase(errorRepo),
                getDeviceTypesUseCase = GetDeviceTypesUseCase(errorRepo),
                batchAddDevicesUseCase = BatchAddDevicesUseCase(
                    FakeAiEngine(),
                    AddDeviceUseCase(errorRepo),
                    FindOrCreateDeviceTypeUseCase(errorRepo),
                ),
                featureFlagProvider = FakeFeatureFlagProvider(),
            )

            // Verify no error initially
            assertNull(viewModel.actionError.value)

            val input = DeviceInput(
                name = "Test Device",
                location = "Kitchen",
                typeId = "type-1",
            )

            viewModel.addDevice(input)
            testDispatcher.scheduler.advanceUntilIdle()

            // The error is captured in actionError instead of crashing the app
            assertNotNull(viewModel.actionError.value)
            assertEquals("Simulated addDevice failure", viewModel.actionError.value)
            // The device was NOT added — operation failed
            assertEquals(0, fakeRepo.devices.size)
            // isLoading is back to false
            assertFalse(viewModel.isLoading.value)

            // dismissActionError clears the error
            viewModel.dismissActionError()
            assertNull(viewModel.actionError.value)
        }
}
