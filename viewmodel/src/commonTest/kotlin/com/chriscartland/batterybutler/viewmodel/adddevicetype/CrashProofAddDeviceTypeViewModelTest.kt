package com.chriscartland.batterybutler.viewmodel.adddevicetype

import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.DeviceTypeInput
import com.chriscartland.batterybutler.domain.model.FeatureFlag
import com.chriscartland.batterybutler.domain.model.ai.AiEngine
import com.chriscartland.batterybutler.domain.model.ai.AiMessage
import com.chriscartland.batterybutler.domain.model.ai.ToolHandler
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.domain.repository.FeatureFlagProvider
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.usecase.AddDeviceTypeUseCase
import com.chriscartland.batterybutler.usecase.BatchAddDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.SuggestDeviceIconUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CrashProofAddDeviceTypeViewModelTest {
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
    fun `usedIcons stays empty when getAllDeviceTypes flow throws`() =
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

            // safeStateIn catches the exception — no crash — but usedIcons stays empty
            val finalState = viewModel.uiState.value
            assertTrue(finalState.usedIcons.isEmpty())
        }

    @Test
    fun `addDeviceType has no error handling - exception from repo is not caught`() =
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
                    // addDeviceType() has no try-catch in viewModelScope.launch.
                }
            }

            val viewModel = createViewModel(interceptingRepo)

            val input = DeviceTypeInput(
                name = "Smoke Detector",
                defaultIcon = "smoke",
                batteryType = "9V",
                batteryQuantity = 1,
            )

            viewModel.addDeviceType(input)
            testDispatcher.scheduler.advanceUntilIdle()

            // The repo method WAS called and WOULD have thrown
            assertNotNull(thrownException)
            // The type was NOT added — operation failed
            assertEquals(0, fakeRepo.deviceTypes.size)
            // BUG: There is no error state on the ViewModel — the user gets no
            // feedback that the operation failed. The ViewModel has no error flow.
        }

    private fun createViewModel(repo: DeviceRepository): AddDeviceTypeViewModel =
        AddDeviceTypeViewModel(
            addDeviceTypeUseCase = AddDeviceTypeUseCase(repo),
            batchAddDeviceTypesUseCase = BatchAddDeviceTypesUseCase(TestAiEngine(), repo),
            suggestDeviceIconUseCase = SuggestDeviceIconUseCase(TestAiEngine()),
            getDeviceTypesUseCase = GetDeviceTypesUseCase(repo),
            featureFlagProvider = TestFeatureFlagProvider(),
        )
}

private class TestAiEngine : AiEngine {
    override val isAvailable: Flow<Boolean> = flowOf(true)
    override val compatibility: Flow<Boolean> = flowOf(true)

    override suspend fun generateResponse(
        prompt: String,
        toolHandler: ToolHandler?,
    ): Flow<AiMessage> = emptyFlow()
}

private class TestFeatureFlagProvider(
    private val enabledFlags: Set<FeatureFlag> = setOf(FeatureFlag.AI_BATCH_IMPORT),
) : FeatureFlagProvider {
    override fun isEnabled(flag: FeatureFlag): Boolean = enabledFlags.contains(flag)

    override fun observeEnabled(flag: FeatureFlag): Flow<Boolean> = flowOf(isEnabled(flag))
}
