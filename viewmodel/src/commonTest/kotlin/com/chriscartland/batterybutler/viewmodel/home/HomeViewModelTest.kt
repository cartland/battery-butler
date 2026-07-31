package com.chriscartland.batterybutler.viewmodel.home

import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import com.chriscartland.batterybutler.domain.model.DeviceImageError
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.DispatcherProvider
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.repository.DeviceImageRepository
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.presentationmodel.home.DensityOption
import com.chriscartland.batterybutler.presentationmodel.home.GroupOption
import com.chriscartland.batterybutler.presentationmodel.home.SortOption
import com.chriscartland.batterybutler.testcommon.FakeDeviceImageRepository
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import com.chriscartland.batterybutler.usecase.DismissSyncStatusUseCase
import com.chriscartland.batterybutler.usecase.ExportDataUseCase
import com.chriscartland.batterybutler.usecase.GetCachedDeviceImageUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.GetDevicesUseCase
import com.chriscartland.batterybutler.usecase.GetSyncStatusUseCase
import com.chriscartland.batterybutler.usecase.ResyncUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class HomeViewModelTest {
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
    fun `initial state is empty`() =
        runTest {
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo)

            val state = viewModel.uiState.first()

            assertEquals(
                0,
                state.groupedDevices.values
                    .flatten()
                    .size,
            )
        }

    @Test
    fun `loads devices and types`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(id = "1", name = "Test Device", typeId = "type-1")
            val type = DeviceType(id = "type-1", name = "Test Type")
            repo.setDevices(listOf(device))
            repo.setDeviceTypes(listOf(type))

            val viewModel = createViewModel(repo)

            val state = viewModel.uiState.first {
                it.groupedDevices.values
                    .flatten()
                    .isNotEmpty()
            }

            assertEquals(
                1,
                state.groupedDevices.values
                    .flatten()
                    .size,
            )
            assertEquals(device, state.groupedDevices.values.flatten()[0])
            assertEquals("Test Type", state.deviceTypes["type-1"]?.name)
        }

    @Test
    fun `devices are sorted by battery age - most recently replaced first - by default`() =
        runTest {
            val repo = FakeDeviceRepository()
            val oldest = TestDevices.createDevice(
                id = "1",
                name = "Oldest",
                batteryLastReplaced = Instant.fromEpochMilliseconds(1_000),
            )
            val newest = TestDevices.createDevice(
                id = "2",
                name = "Newest",
                batteryLastReplaced = Instant.fromEpochMilliseconds(3_000),
            )
            val middle = TestDevices.createDevice(
                id = "3",
                name = "Middle",
                batteryLastReplaced = Instant.fromEpochMilliseconds(2_000),
            )
            repo.setDevices(listOf(oldest, newest, middle))

            val viewModel = createViewModel(repo)

            val state = viewModel.uiState.first {
                it.groupedDevices.values
                    .flatten()
                    .size == 3
            }
            val devices = state.groupedDevices.values.flatten()

            assertEquals(SortOption.BATTERY_AGE, state.sortOption)
            assertFalse(state.isSortAscending)
            assertEquals("Newest", devices[0].name)
            assertEquals("Middle", devices[1].name)
            assertEquals("Oldest", devices[2].name)
        }

    @Test
    fun `onSortOptionSelected changes sort option`() =
        runTest {
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo)

            viewModel.onSortOptionSelected(SortOption.LOCATION)

            val state = viewModel.uiState.first { it.sortOption == SortOption.LOCATION }
            assertEquals(SortOption.LOCATION, state.sortOption)
        }

    @Test
    fun `onGroupOptionSelected changes group option`() =
        runTest {
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo)

            viewModel.onGroupOptionSelected(GroupOption.TYPE)

            val state = viewModel.uiState.first { it.groupOption == GroupOption.TYPE }
            assertEquals(GroupOption.TYPE, state.groupOption)
        }

    @Test
    fun `density option defaults to expanded`() =
        runTest {
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo)

            val state = viewModel.uiState.first()

            assertEquals(DensityOption.EXPANDED, state.densityOption)
        }

    @Test
    fun `onDensityOptionSelected changes density option`() =
        runTest {
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo)

            viewModel.onDensityOptionSelected(DensityOption.COMPACT)

            val state = viewModel.uiState.first { it.densityOption == DensityOption.COMPACT }
            assertEquals(DensityOption.COMPACT, state.densityOption)
        }

    /**
     * Density is a pure display toggle: it must not disturb the sort/group work the same
     * `combine` feeds. Guards the two-step combine wiring (five display options inner, exportData
     * outer) against silently dropping or reordering an input.
     */
    @Test
    fun `changing density preserves sort and group selections`() =
        runTest {
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo)

            viewModel.onSortOptionSelected(SortOption.NAME)
            viewModel.onGroupOptionSelected(GroupOption.TYPE)
            viewModel.toggleSortDirection()
            viewModel.onDensityOptionSelected(DensityOption.COMPACT)

            val state = viewModel.uiState.first { it.densityOption == DensityOption.COMPACT }

            assertEquals(SortOption.NAME, state.sortOption)
            assertEquals(GroupOption.TYPE, state.groupOption)
            assertTrue(state.isSortAscending)
        }

    @Test
    fun `toggleSortDirection inverts sort direction`() =
        runTest {
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo)

            val initialState = viewModel.uiState.first()
            assertFalse(initialState.isSortAscending)

            viewModel.toggleSortDirection()

            val updatedState = viewModel.uiState.first { it.isSortAscending }
            assertTrue(updatedState.isSortAscending)
        }

    @Test
    fun `toggleGroupDirection inverts group direction`() =
        runTest {
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo)

            val initialState = viewModel.uiState.first()
            assertTrue(initialState.isGroupAscending)

            viewModel.toggleGroupDirection()

            val updatedState = viewModel.uiState.first { !it.isGroupAscending }
            assertFalse(updatedState.isGroupAscending)
        }

    @Test
    fun `grouping by type groups devices correctly`() =
        runTest {
            val repo = FakeDeviceRepository()
            val type1 = DeviceType(id = "type-1", name = "Smoke Detector")
            val type2 = DeviceType(id = "type-2", name = "Remote")
            val device1 = TestDevices.createDevice(id = "1", name = "Kitchen Smoke", typeId = "type-1")
            val device2 = TestDevices.createDevice(id = "2", name = "Living Room Smoke", typeId = "type-1")
            val device3 = TestDevices.createDevice(id = "3", name = "TV Remote", typeId = "type-2")
            repo.setDeviceTypes(listOf(type1, type2))
            repo.setDevices(listOf(device1, device2, device3))

            val viewModel = createViewModel(repo)
            viewModel.onGroupOptionSelected(GroupOption.TYPE)

            val state = viewModel.uiState.first {
                it.groupOption == GroupOption.TYPE &&
                    it.groupedDevices.values
                        .flatten()
                        .size == 3
            }

            assertTrue(state.groupedDevices.containsKey("Smoke Detector"))
            assertTrue(state.groupedDevices.containsKey("Remote"))
            assertEquals(2, state.groupedDevices["Smoke Detector"]?.size)
            assertEquals(1, state.groupedDevices["Remote"]?.size)
        }

    @Test
    fun `grouping by location groups devices correctly`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device1 = TestDevices.createDevice(id = "1", name = "Device 1", location = "Kitchen")
            val device2 = TestDevices.createDevice(id = "2", name = "Device 2", location = "Kitchen")
            val device3 = TestDevices.createDevice(id = "3", name = "Device 3", location = "Bedroom")
            repo.setDevices(listOf(device1, device2, device3))

            val viewModel = createViewModel(repo)
            viewModel.onGroupOptionSelected(GroupOption.LOCATION)

            val state = viewModel.uiState.first {
                it.groupOption == GroupOption.LOCATION &&
                    it.groupedDevices.values
                        .flatten()
                        .size == 3
            }

            assertTrue(state.groupedDevices.containsKey("Kitchen"))
            assertTrue(state.groupedDevices.containsKey("Bedroom"))
            assertEquals(2, state.groupedDevices["Kitchen"]?.size)
            assertEquals(1, state.groupedDevices["Bedroom"]?.size)
        }

    /**
     * uiState liveness: the device list must render even if image hydration is slow or stuck.
     * The image observation is one Room-query flow per etag `combine`d together, and `combine`
     * withholds until EVERY input emits — pre-fix, a device with an imageEtag whose cache flow
     * hadn't answered yet held the ENTIRE uiState at its initial loading state, devices
     * invisible despite a populated DB. Against the pre-fix code this test times out waiting
     * for the first populated emission.
     */
    @Test
    fun `uiState emits the device list even when the image observation never emits`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(id = "1", name = "Has Image", typeId = "type-1").copy(imageEtag = "etag-1")
            repo.setDevices(listOf(device))
            val viewModel = createViewModel(repo, imageRepository = NeverEmittingImageRepository())

            val state = viewModel.uiState.first {
                it.groupedDevices.values
                    .flatten()
                    .isNotEmpty()
            }

            assertEquals(
                "Has Image",
                state.groupedDevices.values
                    .flatten()
                    .single()
                    .name,
            )
            assertTrue(state.deviceImagesByEtag.isEmpty(), "images hydrate later; the list must not wait for them")
        }

    private val testDispatcherProvider = object : DispatcherProvider {
        private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
        override val default: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val main: CoroutineDispatcher = dispatcher
    }

    private fun createViewModel(
        repo: DeviceRepository,
        imageRepository: DeviceImageRepository = FakeDeviceImageRepository(),
    ): HomeViewModel =
        HomeViewModel(
            getDevicesUseCase = GetDevicesUseCase(repo),
            getDeviceTypesUseCase = GetDeviceTypesUseCase(repo),
            exportDataUseCase = ExportDataUseCase(repo, testDispatcherProvider),
            getSyncStatusUseCase = GetSyncStatusUseCase(repo),
            dismissSyncStatusUseCase = DismissSyncStatusUseCase(repo),
            resyncUseCase = ResyncUseCase(repo),
            getCachedDeviceImageUseCase = GetCachedDeviceImageUseCase(imageRepository),
        )

    /**
     * A [DeviceImageRepository] whose cached-image observation NEVER emits — the worst case of
     * the real chain, where each image is a Room query flow whose first value arrives
     * asynchronously (or, degenerately, not at all).
     */
    private class NeverEmittingImageRepository : DeviceImageRepository {
        override val supported: Flow<Boolean> = MutableStateFlow(false)

        override fun observeCachedImage(imageEtag: String): Flow<DeviceImageBytes?> = flow { awaitCancellation() }

        override suspend fun uploadImage(
            deviceId: String,
            bytes: ByteArray,
            contentType: String,
        ): Result<String, DeviceImageError> = Result.Success("unused")

        override suspend fun deleteImage(deviceId: String): Boolean = true
    }
}
