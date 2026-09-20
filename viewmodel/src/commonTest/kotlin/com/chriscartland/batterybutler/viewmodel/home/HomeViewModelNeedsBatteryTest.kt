package com.chriscartland.batterybutler.viewmodel.home

import com.chriscartland.batterybutler.domain.model.DispatcherProvider
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.domain.repository.NeedsBatteryRepository
import com.chriscartland.batterybutler.presentationmodel.home.SortOption
import com.chriscartland.batterybutler.testcommon.FakeDeviceImageRepository
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.FakeDisplayDensityRepository
import com.chriscartland.batterybutler.testcommon.FakeNeedsBatteryRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
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
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Covers the "needs a new battery" mark's effect on the device list.
 *
 * The ordering tests matter because `sortAndGroup` reverses its sorted list wholesale for
 * descending order: a comparator-based "flagged first" silently becomes "flagged last" the moment
 * the user flips the sort direction, which is why the float happens after sorting instead.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class HomeViewModelNeedsBatteryTest {
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
        needsBatteryRepository: NeedsBatteryRepository,
    ): HomeViewModel =
        HomeViewModel(
            getDevicesUseCase = GetDevicesUseCase(repo),
            getDeviceTypesUseCase = GetDeviceTypesUseCase(repo),
            exportDataUseCase = ExportDataUseCase(repo, testDispatcherProvider),
            getSyncStatusUseCase = GetSyncStatusUseCase(repo),
            dismissSyncStatusUseCase = DismissSyncStatusUseCase(repo),
            resyncUseCase = ResyncUseCase(repo),
            getCachedDeviceImageUseCase = GetCachedDeviceImageUseCase(FakeDeviceImageRepository()),
            getNeedsBatteryDeviceIdsUseCase = GetNeedsBatteryDeviceIdsUseCase(needsBatteryRepository),
            displayDensityRepository = FakeDisplayDensityRepository(),
        )

    /** oldest -> newest by battery age, so the default descending sort yields newest, middle, oldest. */
    private fun threeDevices() =
        listOf(
            TestDevices.createDevice(id = "1", name = "Oldest", batteryLastReplaced = Instant.fromEpochMilliseconds(1_000)),
            TestDevices.createDevice(id = "2", name = "Newest", batteryLastReplaced = Instant.fromEpochMilliseconds(3_000)),
            TestDevices.createDevice(id = "3", name = "Middle", batteryLastReplaced = Instant.fromEpochMilliseconds(2_000)),
        )

    @Test
    fun `flagged device ids are exposed on the screen state`() =
        runTest {
            val repo = FakeDeviceRepository()
            repo.setDevices(threeDevices())
            val viewModel = createViewModel(repo, FakeNeedsBatteryRepository(initialFlagged = setOf("1")))

            val state = viewModel.uiState.first { it.needsBatteryDeviceIds.isNotEmpty() }

            assertEquals(setOf("1"), state.needsBatteryDeviceIds)
        }

    @Test
    fun `a flagged device floats to the top of the default descending sort`() =
        runTest {
            val repo = FakeDeviceRepository()
            repo.setDevices(threeDevices())
            // "Oldest" sorts last by battery age; the mark must override that.
            val viewModel = createViewModel(repo, FakeNeedsBatteryRepository(initialFlagged = setOf("1")))

            val state = viewModel.uiState.first {
                it.groupedDevices.values
                    .flatten()
                    .size == 3 && it.needsBatteryDeviceIds.isNotEmpty()
            }

            assertEquals(
                listOf("Oldest", "Newest", "Middle"),
                state.groupedDevices.values
                    .flatten()
                    .map { it.name },
            )
        }

    /** The regression guard: flipping to ascending must not flip "flagged first" into "flagged last". */
    @Test
    fun `a flagged device stays on top when the sort direction is reversed`() =
        runTest {
            val repo = FakeDeviceRepository()
            repo.setDevices(threeDevices())
            val viewModel = createViewModel(repo, FakeNeedsBatteryRepository(initialFlagged = setOf("2")))

            viewModel.toggleSortDirection()

            // The flag set is seeded empty and arrives a beat later, so wait for the emission that
            // has both the reversed direction and the mark -- the state this test is about.
            val state = viewModel.uiState.first {
                it.isSortAscending &&
                    it.groupedDevices.values
                        .flatten()
                        .size == 3 &&
                    it.needsBatteryDeviceIds.isNotEmpty()
            }

            // Ascending by battery age is Oldest, Middle, Newest -- "Newest" is flagged, so it leads.
            assertEquals(
                listOf("Newest", "Oldest", "Middle"),
                state.groupedDevices.values
                    .flatten()
                    .map { it.name },
            )
        }

    @Test
    fun `unflagged devices keep their chosen order among themselves`() =
        runTest {
            val repo = FakeDeviceRepository()
            repo.setDevices(threeDevices())
            val viewModel = createViewModel(repo, FakeNeedsBatteryRepository())

            viewModel.onSortOptionSelected(SortOption.NAME)

            val state = viewModel.uiState.first {
                it.sortOption == SortOption.NAME && it.groupedDevices.values
                    .flatten()
                    .size == 3
            }

            // Descending by name, entirely undisturbed when nothing is marked.
            assertEquals(
                listOf("Oldest", "Newest", "Middle"),
                state.groupedDevices.values
                    .flatten()
                    .map { it.name },
            )
        }
}
