package com.chriscartland.batterybutler.viewmodel.addbatteryevent

import com.chriscartland.batterybutler.domain.model.FeatureFlag
import com.chriscartland.batterybutler.domain.model.ai.AiEngine
import com.chriscartland.batterybutler.domain.model.ai.AiMessage
import com.chriscartland.batterybutler.domain.model.ai.ToolHandler
import com.chriscartland.batterybutler.domain.repository.FeatureFlagProvider
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import com.chriscartland.batterybutler.usecase.AddBatteryEventUseCase
import com.chriscartland.batterybutler.usecase.BatchAddBatteryEventsUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceDetailUseCase
import com.chriscartland.batterybutler.usecase.GetDevicesUseCase
import com.chriscartland.batterybutler.usecase.UpdateDeviceLastReplacedUseCase
import com.chriscartland.batterybutler.usecase.UpdateDeviceUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AddBatteryEventViewModelTest {
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
    fun `initial devices state is empty list`() =
        runTest {
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo)

            val devices = viewModel.devices.value
            assertTrue(devices.isEmpty())
        }

    @Test
    fun `devices are loaded from repository`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device1 = TestDevices.createDevice(id = "d1", name = "Device 1")
            val device2 = TestDevices.createDevice(id = "d2", name = "Device 2")
            repo.setDevices(listOf(device1, device2))

            val viewModel = createViewModel(repo)

            val devices = viewModel.devices.first { it.size == 2 }
            assertEquals(2, devices.size)
            assertTrue(devices.any { it.name == "Device 1" })
            assertTrue(devices.any { it.name == "Device 2" })
        }

    @Test
    fun `addEvent adds event to repository`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(id = "d1", name = "Test Device")
            repo.setDevices(listOf(device))

            val viewModel = createViewModel(repo)

            val date = Instant.fromEpochMilliseconds(1000)
            viewModel.addEvent(
                deviceId = "d1",
                date = date,
                batteryType = "AA",
                notes = "Test note",
            )
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, repo.events.size)
            assertEquals("d1", repo.events[0].deviceId)
            assertEquals("AA", repo.events[0].batteryType)
            assertEquals("Test note", repo.events[0].notes)
        }

    @Test
    fun `addEvent updates device batteryLastReplaced when event is newer`() =
        runTest {
            val repo = FakeDeviceRepository()
            val oldDate = Instant.fromEpochMilliseconds(1000)
            val device = TestDevices.createDevice(id = "d1", batteryLastReplaced = oldDate)
            repo.setDevices(listOf(device))

            val viewModel = createViewModel(repo)

            val newDate = Instant.fromEpochMilliseconds(5000)
            viewModel.addEvent(
                deviceId = "d1",
                date = newDate,
                batteryType = "AA",
                notes = null,
            )
            testDispatcher.scheduler.advanceUntilIdle()

            // The AddBatteryEventUseCase delegates to UpdateDeviceLastReplacedUseCase
            // which queries events and updates the device if the latest event is newer
            assertEquals(newDate, repo.devices[0].batteryLastReplaced)
        }

    @Test
    fun `clearAiMessages resets message list`() =
        runTest {
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo)

            val initialMessages = viewModel.aiMessages.first()
            assertTrue(initialMessages.isEmpty())

            viewModel.clearAiMessages()
            val afterClear = viewModel.aiMessages.first()
            assertTrue(afterClear.isEmpty())
        }

    private fun createViewModel(repo: FakeDeviceRepository): AddBatteryEventViewModel =
        AddBatteryEventViewModel(
            addBatteryEventUseCase = AddBatteryEventUseCase(repo, UpdateDeviceLastReplacedUseCase(repo)),
            getDevicesUseCase = GetDevicesUseCase(repo),
            getDeviceDetailUseCase = GetDeviceDetailUseCase(repo),
            updateDeviceUseCase = UpdateDeviceUseCase(repo),
            batchAddBatteryEventsUseCase = BatchAddBatteryEventsUseCase(FakeAiEngine(), repo),
            featureFlagProvider = FakeFeatureFlagProvider(),
        )
}

private class FakeAiEngine : AiEngine {
    override val isAvailable: Flow<Boolean> = flowOf(true)
    override val compatibility: Flow<Boolean> = flowOf(true)

    override suspend fun generateResponse(
        prompt: String,
        toolHandler: ToolHandler?,
    ): Flow<AiMessage> = emptyFlow()
}

private class FakeFeatureFlagProvider(
    private val enabledFlags: Set<FeatureFlag> = setOf(FeatureFlag.AI_BATCH_IMPORT),
) : FeatureFlagProvider {
    override fun isEnabled(flag: FeatureFlag): Boolean = enabledFlags.contains(flag)

    override fun observeEnabled(flag: FeatureFlag): Flow<Boolean> = flowOf(isEnabled(flag))
}
