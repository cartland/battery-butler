package com.chriscartland.batterybutler.viewmodel.devicetypes

import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeListUiState
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.PreloadCommonTypesUseCase
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CrashProofDeviceTypeListViewModelTest {
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
    fun `uiState stays at initial value when getAllDeviceTypes flow throws`() =
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
            val state = viewModel.uiState.value

            // Advance coroutines so the upstream flow throws
            testDispatcher.scheduler.advanceUntilIdle()

            // safeStateIn catches the exception — no crash — but the UI is stuck
            // at the initial Success(emptyMap) value forever
            val finalState = viewModel.uiState.value
            assertIs<DeviceTypeListUiState.Success>(finalState)
            assertTrue(finalState.groupedTypes.isEmpty())
        }

    @Test
    fun `preloadCommonTypes has no error handling - exception from repo is not caught`() =
        runTest {
            val fakeRepo = FakeDeviceRepository()
            // Intercept the exception in the repo to prevent it from crashing the test.
            // In production, this exception would propagate uncaught through
            // viewModelScope.launch and crash the app.
            var thrownException: Throwable? = null
            val interceptingRepo = object : DeviceRepository by fakeRepo {
                override suspend fun addDeviceType(type: DeviceType) {
                    val exception = RuntimeException("Simulated addDeviceType failure")
                    thrownException = exception
                    // Don't rethrow — we're documenting the bug, not crashing the test.
                    // In production code, this throw would propagate uncaught because
                    // preloadCommonTypes() has no try-catch in viewModelScope.launch.
                }
            }

            val viewModel = createViewModel(interceptingRepo)

            viewModel.preloadCommonTypes()
            testDispatcher.scheduler.advanceUntilIdle()

            // The repo method WAS called and WOULD have thrown
            assertNotNull(thrownException)
            // No types were added — operation failed
            assertEquals(0, fakeRepo.deviceTypes.size)
            // BUG: There is no error state on the ViewModel — the user gets no
            // feedback that preloading failed. The ViewModel has no error flow.
        }

    private fun createViewModel(repo: DeviceRepository): DeviceTypeListViewModel =
        DeviceTypeListViewModel(
            getDeviceTypesUseCase = GetDeviceTypesUseCase(repo),
            preloadCommonTypesUseCase = PreloadCommonTypesUseCase(repo),
        )
}
