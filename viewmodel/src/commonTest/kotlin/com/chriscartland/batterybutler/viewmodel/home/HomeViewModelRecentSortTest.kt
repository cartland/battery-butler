package com.chriscartland.batterybutler.viewmodel.home

import com.chriscartland.batterybutler.domain.model.DispatcherProvider
import com.chriscartland.batterybutler.domain.model.ListArrangement
import com.chriscartland.batterybutler.domain.model.ListScreen
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.domain.repository.NeedsBatteryRepository
import com.chriscartland.batterybutler.presentationmodel.home.SortOption
import com.chriscartland.batterybutler.testcommon.FakeDeviceImageRepository
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.FakeDisplayDensityRepository
import com.chriscartland.batterybutler.testcommon.FakeListArrangementRepository
import com.chriscartland.batterybutler.testcommon.FakeNeedsBatteryRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import com.chriscartland.batterybutler.usecase.DismissSyncStatusUseCase
import com.chriscartland.batterybutler.usecase.ExportDataUseCase
import com.chriscartland.batterybutler.usecase.GetCachedDeviceImageUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.GetDevicesUseCase
import com.chriscartland.batterybutler.usecase.GetNeedsBatteryMarksUseCase
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
 * Covers [SortOption.RECENT]: rank by when the device record was last acted on.
 *
 * "Acted on" is the newer of the device's own `lastUpdated` and the moment it was marked as
 * needing a battery. `batteryLastReplaced` is deliberately excluded -- it is user-chosen and
 * routinely backdated, so it says when the battery was changed, not when the record was touched.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class HomeViewModelRecentSortTest {
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

    /** Sort by RECENT, ungrouped, so assertions read as one flat ordering. */
    private fun recentArrangement() =
        FakeListArrangementRepository(
            initial = mapOf(ListScreen.DEVICES to ListArrangement(sortKey = "recent", groupKey = "none")),
        )

    private fun createViewModel(
        repo: DeviceRepository,
        needsBatteryRepository: NeedsBatteryRepository,
        arrangements: FakeListArrangementRepository = recentArrangement(),
    ): HomeViewModel =
        HomeViewModel(
            getDevicesUseCase = GetDevicesUseCase(repo),
            getDeviceTypesUseCase = GetDeviceTypesUseCase(repo),
            exportDataUseCase = ExportDataUseCase(repo, testDispatcherProvider),
            getSyncStatusUseCase = GetSyncStatusUseCase(repo),
            dismissSyncStatusUseCase = DismissSyncStatusUseCase(repo),
            resyncUseCase = ResyncUseCase(repo),
            getCachedDeviceImageUseCase = GetCachedDeviceImageUseCase(FakeDeviceImageRepository()),
            getNeedsBatteryMarksUseCase = GetNeedsBatteryMarksUseCase(needsBatteryRepository),
            displayDensityRepository = FakeDisplayDensityRepository(),
            listArrangementRepository = arrangements,
        )

    private fun at(ms: Long) = Instant.fromEpochMilliseconds(ms)

    /**
     * Settles once the list is fully hydrated, then returns the order.
     *
     * Both conditions are order-independent on purpose. Waiting for "the first item is X" would
     * hang forever when the order is wrong instead of failing, and a hanging test is
     * indistinguishable from a slow one in CI.
     */
    private suspend fun HomeViewModel.namesInOrder(
        expectedCount: Int,
        expectedMarks: Int = 0,
    ): List<String> =
        uiState
            .first { state ->
                state.sortOption == SortOption.RECENT &&
                    state.groupedDevices.values
                        .flatten()
                        .size == expectedCount &&
                    state.needsBatteryDeviceIds.size == expectedMarks
            }.groupedDevices.values
            .flatten()
            .map { it.name }

    @Test
    fun `most recently updated device leads`() =
        runTest {
            val repo = FakeDeviceRepository()
            repo.setDevices(
                listOf(
                    TestDevices.createDevice(id = "1", name = "Stale", lastUpdated = at(1_000)),
                    TestDevices.createDevice(id = "3", name = "Freshest", lastUpdated = at(3_000)),
                    TestDevices.createDevice(id = "2", name = "Middle", lastUpdated = at(2_000)),
                ),
            )

            val names = createViewModel(repo, FakeNeedsBatteryRepository()).namesInOrder(expectedCount = 3)

            assertEquals(listOf("Freshest", "Middle", "Stale"), names)
        }

    /**
     * The headline case: when a device was marked is part of its recency.
     *
     * Both devices are marked deliberately. `sortAndGroup`'s `priorityFirst` floats *any* marked
     * device to the front regardless of the comparator, so a single marked device would land
     * first even if recency ignored marks entirely -- the assertion would pass for the wrong
     * reason. With both floated, the surviving differentiator is the mark timestamp: "JustMarked"
     * has the older `lastUpdated` and must still lead.
     */
    @Test
    fun `among marked devices the one marked most recently leads`() =
        runTest {
            val repo = FakeDeviceRepository()
            repo.setDevices(
                listOf(
                    TestDevices.createDevice(id = "1", name = "JustMarked", lastUpdated = at(1_000)),
                    TestDevices.createDevice(id = "2", name = "MarkedEarlier", lastUpdated = at(2_000)),
                ),
            )
            val marks = FakeNeedsBatteryRepository(
                initialMarks = mapOf("1" to at(9_000), "2" to at(3_000)),
            )

            val names = createViewModel(repo, marks).namesInOrder(expectedCount = 2, expectedMarks = 2)

            assertEquals(listOf("JustMarked", "MarkedEarlier"), names)
        }

    /**
     * A backdated battery replacement must not drag a device down the list. This is the reason
     * `batteryLastReplaced` is not part of the recency calculation at all.
     */
    @Test
    fun `an old batteryLastReplaced does not sink a recently edited device`() =
        runTest {
            val repo = FakeDeviceRepository()
            repo.setDevices(
                listOf(
                    TestDevices.createDevice(
                        id = "1",
                        name = "Backdated",
                        batteryLastReplaced = at(1),
                        lastUpdated = at(9_000),
                    ),
                    TestDevices.createDevice(
                        id = "2",
                        name = "FreshBattery",
                        batteryLastReplaced = at(8_000),
                        lastUpdated = at(2_000),
                    ),
                ),
            )

            val names = createViewModel(repo, FakeNeedsBatteryRepository()).namesInOrder(expectedCount = 2)

            assertEquals(listOf("Backdated", "FreshBattery"), names)
        }

    /** An unmarked device falls back to its own lastUpdated rather than being treated as missing. */
    @Test
    fun `unmarked devices still rank by their own lastUpdated`() =
        runTest {
            val repo = FakeDeviceRepository()
            repo.setDevices(
                listOf(
                    TestDevices.createDevice(id = "1", name = "Old", lastUpdated = at(1_000)),
                    TestDevices.createDevice(id = "2", name = "New", lastUpdated = at(4_000)),
                ),
            )
            val marks = FakeNeedsBatteryRepository(initialMarks = mapOf("nobody" to at(9_999)))

            val names = createViewModel(repo, marks).namesInOrder(expectedCount = 2, expectedMarks = 1)

            assertEquals(listOf("New", "Old"), names)
        }

    @Test
    fun `the recent choice round-trips through storage`() =
        runTest {
            val repo = FakeDeviceRepository()
            val arrangements = FakeListArrangementRepository()

            createViewModel(repo, FakeNeedsBatteryRepository(), arrangements)
                .onSortOptionSelected(SortOption.RECENT)
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert on the store first: the restart check below waits for a matching emission,
            // which would hang rather than fail if the write never happened.
            assertEquals("recent", arrangements.arrangement(ListScreen.DEVICES).first().sortKey)

            val restarted = createViewModel(repo, FakeNeedsBatteryRepository(), arrangements)
            assertEquals(
                SortOption.RECENT,
                restarted.uiState.first { it.sortOption == SortOption.RECENT }.sortOption,
            )
        }
}
