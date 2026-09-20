package com.chriscartland.batterybutler.viewmodel.home

import com.chriscartland.batterybutler.domain.model.DispatcherProvider
import com.chriscartland.batterybutler.domain.model.ListArrangement
import com.chriscartland.batterybutler.domain.model.ListScreen
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.presentationmodel.home.GroupOption
import com.chriscartland.batterybutler.presentationmodel.home.SortOption
import com.chriscartland.batterybutler.testcommon.FakeDeviceImageRepository
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.FakeDisplayDensityRepository
import com.chriscartland.batterybutler.testcommon.FakeListArrangementRepository
import com.chriscartland.batterybutler.testcommon.FakeNeedsBatteryRepository
import com.chriscartland.batterybutler.usecase.DismissSyncStatusUseCase
import com.chriscartland.batterybutler.usecase.ExportDataUseCase
import com.chriscartland.batterybutler.usecase.GetCachedDeviceImageUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.GetDevicesUseCase
import com.chriscartland.batterybutler.usecase.GetNeedsBatteryDeviceIdsUseCase
import com.chriscartland.batterybutler.usecase.GetSyncStatusUseCase
import com.chriscartland.batterybutler.usecase.ResyncUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers the sort/group choices surviving a restart.
 *
 * "Restart" is modelled as a second [HomeViewModel] built over the same repository — a new
 * ViewModel with no in-memory state, reading only what was stored.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelArrangementTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val testDispatcherProvider = object : DispatcherProvider {
        private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
        override val default: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val main: CoroutineDispatcher = dispatcher
    }

    private fun createViewModel(
        repo: DeviceRepository,
        arrangementRepository: FakeListArrangementRepository,
    ): HomeViewModel =
        HomeViewModel(
            getDevicesUseCase = GetDevicesUseCase(repo),
            getDeviceTypesUseCase = GetDeviceTypesUseCase(repo),
            exportDataUseCase = ExportDataUseCase(repo, testDispatcherProvider),
            getSyncStatusUseCase = GetSyncStatusUseCase(repo),
            dismissSyncStatusUseCase = DismissSyncStatusUseCase(repo),
            resyncUseCase = ResyncUseCase(repo),
            getCachedDeviceImageUseCase = GetCachedDeviceImageUseCase(FakeDeviceImageRepository()),
            getNeedsBatteryDeviceIdsUseCase = GetNeedsBatteryDeviceIdsUseCase(FakeNeedsBatteryRepository()),
            displayDensityRepository = FakeDisplayDensityRepository(),
            listArrangementRepository = arrangementRepository,
        )

    @Test
    fun `defaults match the behaviour before these choices were persisted`() =
        runTest {
            val viewModel = createViewModel(FakeDeviceRepository(), FakeListArrangementRepository())

            val state = viewModel.uiState.first()

            assertEquals(SortOption.BATTERY_AGE, state.sortOption)
            assertEquals(GroupOption.NONE, state.groupOption)
            assertFalse(state.isSortAscending)
            assertTrue(state.isGroupAscending)
        }

    @Test
    fun `a stored arrangement is applied on start`() =
        runTest {
            val arrangements = FakeListArrangementRepository(
                initial = mapOf(
                    ListScreen.DEVICES to ListArrangement(
                        sortKey = "name",
                        groupKey = "location",
                        isSortAscending = true,
                        isGroupAscending = false,
                    ),
                ),
            )

            val state = createViewModel(FakeDeviceRepository(), arrangements)
                .uiState
                .first { it.sortOption == SortOption.NAME }

            assertEquals(GroupOption.LOCATION, state.groupOption)
            assertTrue(state.isSortAscending)
            assertFalse(state.isGroupAscending)
        }

    @Test
    fun `selecting a sort survives into a new ViewModel`() =
        runTest {
            val repo = FakeDeviceRepository()
            val arrangements = FakeListArrangementRepository()

            createViewModel(repo, arrangements).onSortOptionSelected(SortOption.LOCATION)
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert on the store directly first. The restart check below waits for a matching
            // emission, which would hang rather than fail if the write never happened.
            assertEquals("location", arrangements.arrangement(ListScreen.DEVICES).first().sortKey)

            val restarted = createViewModel(repo, arrangements)
            assertEquals(
                SortOption.LOCATION,
                restarted.uiState.first { it.sortOption == SortOption.LOCATION }.sortOption,
            )
        }

    @Test
    fun `selecting a grouping survives into a new ViewModel`() =
        runTest {
            val repo = FakeDeviceRepository()
            val arrangements = FakeListArrangementRepository()

            createViewModel(repo, arrangements).onGroupOptionSelected(GroupOption.TYPE)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("type", arrangements.arrangement(ListScreen.DEVICES).first().groupKey)

            val restarted = createViewModel(repo, arrangements)
            assertEquals(
                GroupOption.TYPE,
                restarted.uiState.first { it.groupOption == GroupOption.TYPE }.groupOption,
            )
        }

    @Test
    fun `both direction toggles survive into a new ViewModel`() =
        runTest {
            val repo = FakeDeviceRepository()
            val arrangements = FakeListArrangementRepository()

            // Defaults are sort-descending and group-ascending, so one flip of each inverts both.
            val viewModel = createViewModel(repo, arrangements)
            viewModel.toggleSortDirection()
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.toggleGroupDirection()
            testDispatcher.scheduler.advanceUntilIdle()

            val stored = arrangements.arrangement(ListScreen.DEVICES).first()
            assertEquals(true, stored.isSortAscending)
            assertEquals(false, stored.isGroupAscending)

            val restarted = createViewModel(repo, arrangements)
            val state = restarted.uiState.first { it.isSortAscending }
            assertTrue(state.isSortAscending)
            assertFalse(state.isGroupAscending)
        }

    /** An unknown token — a downgrade, a hand-edited store — must degrade to the default. */
    @Test
    fun `an unrecognised stored sort falls back to the default`() =
        runTest {
            val arrangements = FakeListArrangementRepository(
                initial = mapOf(ListScreen.DEVICES to ListArrangement(sortKey = "not-a-sort")),
            )

            val state = createViewModel(FakeDeviceRepository(), arrangements).uiState.first()

            assertEquals(SortOption.BATTERY_AGE, state.sortOption)
        }

    /** The device list must not read the device-type list's stored choices. */
    @Test
    fun `the device-type arrangement does not leak into the device list`() =
        runTest {
            val arrangements = FakeListArrangementRepository(
                initial = mapOf(
                    ListScreen.DEVICE_TYPES to ListArrangement(sortKey = "battery_type", groupKey = "battery_type"),
                ),
            )

            val state = createViewModel(FakeDeviceRepository(), arrangements).uiState.first()

            assertEquals(SortOption.BATTERY_AGE, state.sortOption)
            assertEquals(GroupOption.NONE, state.groupOption)
        }
}
