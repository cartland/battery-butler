package com.chriscartland.batterybutler.viewmodel.adddevicetype

import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.DeviceTypeInput
import com.chriscartland.batterybutler.domain.model.FeatureFlag
import com.chriscartland.batterybutler.domain.model.Result
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
    fun `actionError is set when getAllDeviceTypes flow throws`() =
        runTest {
            val fakeRepo = FakeDeviceRepository()
            val throwingRepo = object : DeviceRepository by fakeRepo {
                override fun getAllDeviceTypes(): Flow<List<DeviceType>> =
                    flow {
                        throw RuntimeException("Simulated getAllDeviceTypes failure")
                    }
            }

            val viewModel = createViewModel(throwingRepo)

            // Actively subscribe to trigger the stateIn flow collection
            val job = launch { viewModel.uiState.collect {} }

            // Advance coroutines so the upstream flow throws
            testDispatcher.scheduler.advanceUntilIdle()

            // safeStateIn catches the exception and routes error to actionError
            assertNotNull(viewModel.actionError.value)
            assertEquals("Simulated getAllDeviceTypes failure", viewModel.actionError.value)

            // usedIcons falls back to empty
            assertTrue(
                viewModel.uiState.value.usedIcons
                    .isEmpty(),
            )

            job.cancel()
        }

    @Test
    fun `addDeviceType sets actionError when repo returns error`() =
        runTest {
            val fakeRepo = FakeDeviceRepository()
            val errorRepo = object : DeviceRepository by fakeRepo {
                override suspend fun addDeviceType(type: DeviceType): Result<Unit, DataError> = Result.Error(DataError.Database.WriteFailed(message = "Simulated addDeviceType failure"))
            }

            val viewModel = createViewModel(errorRepo)

            // Verify no error initially
            assertNull(viewModel.actionError.value)

            val input = DeviceTypeInput(
                name = "Smoke Detector",
                defaultIcon = "smoke",
                batteryType = "9V",
                batteryQuantity = 1,
            )

            viewModel.addDeviceType(input)
            testDispatcher.scheduler.advanceUntilIdle()

            // The error is captured in actionError instead of crashing the app
            assertNotNull(viewModel.actionError.value)
            assertEquals("Simulated addDeviceType failure", viewModel.actionError.value)

            // The type was NOT added — operation failed
            assertEquals(0, fakeRepo.deviceTypes.size)

            // dismissActionError clears the error
            viewModel.dismissActionError()
            assertNull(viewModel.actionError.value)
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
