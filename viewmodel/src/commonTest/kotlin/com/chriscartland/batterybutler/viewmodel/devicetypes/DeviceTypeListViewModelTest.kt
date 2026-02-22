package com.chriscartland.batterybutler.viewmodel.devicetypes

import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeGroupOption
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeListUiState
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeSortOption
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.PreloadCommonTypesUseCase
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceTypeListViewModelTest {
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
    fun `initial state is Success with empty map`() =
        runTest {
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo)

            val state = viewModel.uiState.value
            assertIs<DeviceTypeListUiState.Success>(state)
            assertTrue(state.groupedTypes.isEmpty())
        }

    @Test
    fun `types loaded and sorted by name ascending by default`() =
        runTest {
            val repo = FakeDeviceRepository()
            val typeZ = TestDevices.createDeviceType(id = "t1", name = "Zulu Type", batteryType = "AA")
            val typeA = TestDevices.createDeviceType(id = "t2", name = "Alpha Type", batteryType = "AAA")
            val typeM = TestDevices.createDeviceType(id = "t3", name = "Mike Type", batteryType = "AA")
            repo.setDeviceTypes(listOf(typeZ, typeA, typeM))

            val viewModel = createViewModel(repo)

            val state = viewModel.uiState.first {
                it is DeviceTypeListUiState.Success &&
                    it.groupedTypes.values
                        .flatten()
                        .size == 3
            }
            assertIs<DeviceTypeListUiState.Success>(state)
            val types = state.groupedTypes.values.flatten()
            assertEquals("Alpha Type", types[0].name)
            assertEquals("Mike Type", types[1].name)
            assertEquals("Zulu Type", types[2].name)
        }

    @Test
    fun `sort by battery type groups correctly`() =
        runTest {
            val repo = FakeDeviceRepository()
            val type1 = TestDevices.createDeviceType(id = "t1", name = "Device B", batteryType = "AAA")
            val type2 = TestDevices.createDeviceType(id = "t2", name = "Device A", batteryType = "AA")
            repo.setDeviceTypes(listOf(type1, type2))

            val viewModel = createViewModel(repo)
            viewModel.onSortOptionSelected(DeviceTypeSortOption.BATTERY_TYPE)

            val state = viewModel.uiState.first {
                it is DeviceTypeListUiState.Success &&
                    it.sortOption == DeviceTypeSortOption.BATTERY_TYPE &&
                    it.groupedTypes.values
                        .flatten()
                        .size == 2
            }
            assertIs<DeviceTypeListUiState.Success>(state)
            val types = state.groupedTypes.values.flatten()
            // Sorted by batteryType first, then name
            assertEquals("Device A", types[0].name) // AA
            assertEquals("Device B", types[1].name) // AAA
        }

    @Test
    fun `toggleSortDirection reverses sort order`() =
        runTest {
            val repo = FakeDeviceRepository()
            val typeA = TestDevices.createDeviceType(id = "t1", name = "Alpha", batteryType = "AA")
            val typeZ = TestDevices.createDeviceType(id = "t2", name = "Zulu", batteryType = "AA")
            repo.setDeviceTypes(listOf(typeA, typeZ))

            val viewModel = createViewModel(repo)

            // Default is ascending
            val ascState = viewModel.uiState.first {
                it is DeviceTypeListUiState.Success &&
                    it.groupedTypes.values
                        .flatten()
                        .size == 2
            }
            assertIs<DeviceTypeListUiState.Success>(ascState)
            assertTrue(ascState.isSortAscending)
            assertEquals(
                "Alpha",
                ascState.groupedTypes.values
                    .flatten()[0]
                    .name,
            )

            viewModel.toggleSortDirection()

            val descState = viewModel.uiState.first {
                it is DeviceTypeListUiState.Success && !it.isSortAscending
            }
            assertIs<DeviceTypeListUiState.Success>(descState)
            assertFalse(descState.isSortAscending)
            assertEquals(
                "Zulu",
                descState.groupedTypes.values
                    .flatten()[0]
                    .name,
            )
        }

    @Test
    fun `group by battery type creates separate groups`() =
        runTest {
            val repo = FakeDeviceRepository()
            val type1 = TestDevices.createDeviceType(id = "t1", name = "Smoke Detector", batteryType = "9V")
            val type2 = TestDevices.createDeviceType(id = "t2", name = "Remote", batteryType = "AAA")
            val type3 = TestDevices.createDeviceType(id = "t3", name = "Clock", batteryType = "AA")
            val type4 = TestDevices.createDeviceType(id = "t4", name = "Flashlight", batteryType = "AA")
            repo.setDeviceTypes(listOf(type1, type2, type3, type4))

            val viewModel = createViewModel(repo)
            viewModel.onGroupOptionSelected(DeviceTypeGroupOption.BATTERY_TYPE)

            val state = viewModel.uiState.first {
                it is DeviceTypeListUiState.Success &&
                    it.groupOption == DeviceTypeGroupOption.BATTERY_TYPE &&
                    it.groupedTypes.values
                        .flatten()
                        .size == 4
            }
            assertIs<DeviceTypeListUiState.Success>(state)
            assertTrue(state.groupedTypes.containsKey("9V"))
            assertTrue(state.groupedTypes.containsKey("AAA"))
            assertTrue(state.groupedTypes.containsKey("AA"))
            assertEquals(1, state.groupedTypes["9V"]?.size)
            assertEquals(1, state.groupedTypes["AAA"]?.size)
            assertEquals(2, state.groupedTypes["AA"]?.size)
        }

    @Test
    fun `toggleGroupDirection inverts group sort order`() =
        runTest {
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo)

            val initialState = viewModel.uiState.first()
            assertIs<DeviceTypeListUiState.Success>(initialState)
            assertTrue(initialState.isGroupAscending)

            viewModel.toggleGroupDirection()

            val updatedState = viewModel.uiState.first {
                it is DeviceTypeListUiState.Success && !it.isGroupAscending
            }
            assertIs<DeviceTypeListUiState.Success>(updatedState)
            assertFalse(updatedState.isGroupAscending)
        }

    private fun createViewModel(repo: FakeDeviceRepository): DeviceTypeListViewModel =
        DeviceTypeListViewModel(
            getDeviceTypesUseCase = GetDeviceTypesUseCase(repo),
            preloadCommonTypesUseCase = PreloadCommonTypesUseCase(repo),
        )
}
