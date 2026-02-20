package com.chriscartland.batterybutler.viewmodel.adddevicetype

import com.chriscartland.batterybutler.domain.model.DeviceTypeInput
import com.chriscartland.batterybutler.domain.model.FeatureFlag
import com.chriscartland.batterybutler.domain.model.ai.AiEngine
import com.chriscartland.batterybutler.domain.model.ai.AiMessage
import com.chriscartland.batterybutler.domain.model.ai.ToolHandler
import com.chriscartland.batterybutler.domain.repository.FeatureFlagProvider
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import com.chriscartland.batterybutler.usecase.AddDeviceTypeUseCase
import com.chriscartland.batterybutler.usecase.BatchAddDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.SuggestDeviceIconUseCase
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AddDeviceTypeViewModelTest {
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
    fun `initial ui state has default values`() =
        runTest {
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo)

            val state = viewModel.uiState.value
            assertTrue(state.aiMessages.isEmpty())
            assertTrue(state.usedIcons.isEmpty())
            assertFalse(state.isSuggestingIcon)
        }

    @Test
    fun `addDeviceType adds type to repository`() =
        runTest {
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo)

            val input = DeviceTypeInput(
                name = "Smoke Detector",
                defaultIcon = "smoke",
                batteryType = "9V",
                batteryQuantity = 1,
            )
            viewModel.addDeviceType(input)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, repo.deviceTypes.size)
            assertEquals("Smoke Detector", repo.deviceTypes[0].name)
            assertEquals("smoke", repo.deviceTypes[0].defaultIcon)
            assertEquals("9V", repo.deviceTypes[0].batteryType)
            assertEquals(1, repo.deviceTypes[0].batteryQuantity)
        }

    @Test
    fun `clearAiMessages resets message list`() =
        runTest {
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo)

            viewModel.clearAiMessages()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.first()
            assertTrue(state.aiMessages.isEmpty())
        }

    @Test
    fun `usedIcons reflects existing device types`() =
        runTest {
            val repo = FakeDeviceRepository()
            val type1 = TestDevices.createDeviceType(id = "t1", name = "Type 1", defaultIcon = "icon_a")
            val type2 = TestDevices.createDeviceType(id = "t2", name = "Type 2", defaultIcon = "icon_b")
            repo.setDeviceTypes(listOf(type1, type2))

            val viewModel = createViewModel(repo)

            val state = viewModel.uiState.first { it.usedIcons.isNotEmpty() }
            assertTrue(state.usedIcons.contains("icon_a"))
            assertTrue(state.usedIcons.contains("icon_b"))
        }

    private fun createViewModel(repo: FakeDeviceRepository): AddDeviceTypeViewModel =
        AddDeviceTypeViewModel(
            addDeviceTypeUseCase = AddDeviceTypeUseCase(repo),
            batchAddDeviceTypesUseCase = BatchAddDeviceTypesUseCase(FakeAiEngine(), repo),
            suggestDeviceIconUseCase = SuggestDeviceIconUseCase(FakeAiEngine()),
            getDeviceTypesUseCase = GetDeviceTypesUseCase(repo),
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
