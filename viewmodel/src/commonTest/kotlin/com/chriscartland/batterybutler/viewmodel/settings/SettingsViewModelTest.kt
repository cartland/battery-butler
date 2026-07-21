package com.chriscartland.batterybutler.viewmodel.settings

import com.chriscartland.batterybutler.domain.model.AppVersion
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.DevServerUrl
import com.chriscartland.batterybutler.domain.model.DispatcherProvider
import com.chriscartland.batterybutler.domain.model.FeatureFlag
import com.chriscartland.batterybutler.domain.model.LabsProdUrl
import com.chriscartland.batterybutler.domain.model.LabsStagingUrl
import com.chriscartland.batterybutler.domain.model.LegacyDatabaseInfo
import com.chriscartland.batterybutler.domain.model.ProductionServerUrl
import com.chriscartland.batterybutler.domain.model.RestoreResult
import com.chriscartland.batterybutler.domain.model.User
import com.chriscartland.batterybutler.domain.model.ai.AiEngineType
import com.chriscartland.batterybutler.domain.repository.AiPreferencesRepository
import com.chriscartland.batterybutler.domain.repository.AppInfoRepository
import com.chriscartland.batterybutler.domain.repository.DataModeRepository
import com.chriscartland.batterybutler.domain.repository.FeatureFlagProvider
import com.chriscartland.batterybutler.domain.repository.RestartCoordinator
import com.chriscartland.batterybutler.testcommon.FakeAuthRepository
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.FakeLabsAuthRepository
import com.chriscartland.batterybutler.testcommon.FakeLegacyDatabaseRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import com.chriscartland.batterybutler.usecase.ExportDataUseCase
import com.chriscartland.batterybutler.usecase.GetAppVersionUseCase
import com.chriscartland.batterybutler.usecase.GetLegacyDatabaseInfoUseCase
import com.chriscartland.batterybutler.usecase.ImportDataUseCase
import com.chriscartland.batterybutler.usecase.RestoreLegacyDatabaseUseCase
import com.chriscartland.batterybutler.usecase.SignInToLabsUseCase
import com.chriscartland.batterybutler.usecase.SignOutLabsUseCase
import com.chriscartland.batterybutler.usecase.SignOutUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    // App-lifetime stand-in for SignInToLabsUseCase's injected scope; runs on the test dispatcher
    // so launched resyncs execute under the test's virtual time.
    private val useCaseScope = kotlinx.coroutines.CoroutineScope(testDispatcher + kotlinx.coroutines.SupervisorJob())

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region Fake implementations

    private class FakeDataModeRepository : DataModeRepository {
        private val _dataMode = MutableStateFlow<DataMode>(DataMode.None)
        override val dataMode: Flow<DataMode> = _dataMode
        var setDataModeCallCount = 0
            private set
        var lastSetMode: DataMode? = null
            private set

        override suspend fun setDataMode(mode: DataMode) {
            setDataModeCallCount++
            lastSetMode = mode
            _dataMode.value = mode
        }

        fun setCurrentMode(mode: DataMode) {
            _dataMode.value = mode
        }
    }

    private class FakeAppInfoRepository(
        private val version: AppVersion = AppVersion.Unavailable,
    ) : AppInfoRepository {
        override fun getAppVersion(): AppVersion = version
    }

    private class FakeFeatureFlagProvider(
        private val enabledFlags: Set<FeatureFlag> = emptySet(),
    ) : FeatureFlagProvider {
        override fun isEnabled(flag: FeatureFlag): Boolean = enabledFlags.contains(flag)

        override fun observeEnabled(flag: FeatureFlag): Flow<Boolean> = MutableStateFlow(isEnabled(flag))
    }

    private class FakeAiPreferencesRepository : AiPreferencesRepository {
        private val _aiEngineType = MutableStateFlow(AiEngineType.Cloud)
        override val aiEngineType: Flow<AiEngineType> = _aiEngineType
        var setAiEngineTypeCallCount = 0
            private set

        override suspend fun setAiEngineType(type: AiEngineType) {
            setAiEngineTypeCallCount++
            _aiEngineType.value = type
        }
    }

    private val testDispatcherProvider = object : DispatcherProvider {
        private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
        override val default: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val main: CoroutineDispatcher = dispatcher
    }

    // endregion

    // region Auth tests

    @Test
    fun `currentUser is null when unauthenticated`() =
        runTest {
            val authRepo = FakeAuthRepository()
            authRepo.setAuthState(AuthState.Unauthenticated())
            val viewModel = createViewModel(authRepository = authRepo)
            advanceUntilIdle()

            val user = viewModel.currentUser.first()

            assertNull(user)
        }

    @Test
    fun `currentUser has user when authenticated`() =
        runTest {
            val authRepo = FakeAuthRepository()
            val testUser = User(
                id = "user-1",
                email = "test@example.com",
                displayName = "Test User",
                photoUrl = null,
            )
            authRepo.setAuthState(AuthState.Authenticated(testUser))
            val viewModel = createViewModel(authRepository = authRepo)
            advanceUntilIdle()

            val user = viewModel.currentUser.first { it != null }

            assertNotNull(user)
            assertEquals("user-1", user.id)
            assertEquals("test@example.com", user.email)
        }

    @Test
    fun `isLabsMode reflects the selected data mode`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()
            assertFalse(viewModel.isLabsMode.first())

            viewModel.onDataModeSelected(DataMode.LabsStaging("https://staging.example"))
            advanceUntilIdle()
            assertTrue(viewModel.isLabsMode.first { it })
        }

    @Test
    fun `onSignInToLabs signs in via the labs repository and triggers a resync`() =
        runTest {
            val labsRepo = FakeLabsAuthRepository()
            val deviceRepo = FakeDeviceRepository()
            val viewModel = createViewModel(labsAuthRepository = labsRepo, deviceRepository = deviceRepo)
            advanceUntilIdle()

            viewModel.onSignInToLabs()
            advanceUntilIdle()

            assertEquals(1, labsRepo.signInCount)
            assertTrue(viewModel.isLabsSignedIn.first { it })
            assertEquals(1, deviceRepo.resyncCount)
        }

    @Test
    fun `onSignOutLabs clears the labs session and the locally cached data`() =
        runTest {
            val labsRepo = FakeLabsAuthRepository()
            labsRepo.setLabsAuthState(
                AuthState.Authenticated(
                    User(id = "u", email = "labs@example.com", displayName = null, photoUrl = null),
                ),
            )
            val deviceRepo = FakeDeviceRepository()
            deviceRepo.setDevices(listOf(TestDevices.createDevice(id = "cached-device")))
            val viewModel = createViewModel(labsAuthRepository = labsRepo, deviceRepository = deviceRepo)
            advanceUntilIdle()
            assertTrue(viewModel.isLabsSignedIn.first { it })

            viewModel.onSignOutLabs()
            advanceUntilIdle()

            assertEquals(1, labsRepo.signOutCount)
            assertFalse(viewModel.isLabsSignedIn.first { !it })
            assertEquals(1, deviceRepo.clearAllLocalDataCount)
            assertTrue(deviceRepo.getAllDevices().first().isEmpty())
        }

    @Test
    fun `isSignedIn is false when unauthenticated`() =
        runTest {
            val authRepo = FakeAuthRepository()
            authRepo.setAuthState(AuthState.Unauthenticated())
            val viewModel = createViewModel(authRepository = authRepo)
            advanceUntilIdle()

            val signedIn = viewModel.isSignedIn.first()

            assertFalse(signedIn)
        }

    @Test
    fun `isSignedIn is true when authenticated`() =
        runTest {
            val authRepo = FakeAuthRepository()
            val testUser = User(
                id = "user-1",
                email = "test@example.com",
                displayName = "Test",
                photoUrl = null,
            )
            authRepo.setAuthState(AuthState.Authenticated(testUser))
            val viewModel = createViewModel(authRepository = authRepo)
            advanceUntilIdle()

            val signedIn = viewModel.isSignedIn.first { it }

            assertTrue(signedIn)
        }

    @Test
    fun `signOut calls auth repository and clears the locally cached data`() =
        runTest {
            val authRepo = FakeAuthRepository()
            val deviceRepo = FakeDeviceRepository()
            deviceRepo.setDevices(listOf(TestDevices.createDevice(id = "cached-device")))
            val viewModel = createViewModel(authRepository = authRepo, deviceRepository = deviceRepo)

            viewModel.signOut()
            advanceUntilIdle()

            assertEquals(1, authRepo.signOutCallCount)
            assertEquals(1, deviceRepo.clearAllLocalDataCount)
            assertTrue(deviceRepo.getAllDevices().first().isEmpty())
        }

    // endregion

    // region Data mode tests

    @Test
    fun `dataMode reflects repository state`() =
        runTest {
            val networkRepo = FakeDataModeRepository()
            networkRepo.setCurrentMode(DataMode.Mock)
            val viewModel = createViewModel(dataModeRepository = networkRepo)
            advanceUntilIdle()

            val mode = viewModel.dataMode.first { it == DataMode.Mock }

            assertEquals(DataMode.Mock, mode)
        }

    @Test
    fun `onDataModeSelected updates repository`() =
        runTest {
            val networkRepo = FakeDataModeRepository()
            val viewModel = createViewModel(dataModeRepository = networkRepo)

            viewModel.onDataModeSelected(DataMode.Mock)
            advanceUntilIdle()

            assertEquals(1, networkRepo.setDataModeCallCount)
            assertEquals(DataMode.Mock, networkRepo.lastSetMode)
        }

    // endregion

    // region App version tests

    @Test
    fun `appVersion is set from GetAppVersionUseCase`() =
        runTest {
            val version = AppVersion.Android(versionName = "1.2.3", versionCode = 42)
            val appInfoRepo = FakeAppInfoRepository(version)
            val viewModel = createViewModel(appInfoRepository = appInfoRepo)

            val appVersion = viewModel.appVersion.value

            assertIs<AppVersion.Android>(appVersion)
            assertEquals("1.2.3", appVersion.versionName)
            assertEquals(42, appVersion.versionCode)
        }

    @Test
    fun `appVersion defaults to Unavailable when repository returns Unavailable`() =
        runTest {
            val appInfoRepo = FakeAppInfoRepository(AppVersion.Unavailable)
            val viewModel = createViewModel(appInfoRepository = appInfoRepo)

            val appVersion = viewModel.appVersion.value

            assertIs<AppVersion.Unavailable>(appVersion)
        }

    // endregion

    // region Export data tests

    @Test
    fun `onExportData populates exportData`() =
        runTest {
            val deviceRepo = FakeDeviceRepository()
            val device = TestDevices.createDevice(id = "d1", name = "Test Device")
            deviceRepo.setDevices(listOf(device))
            val viewModel = createViewModel(deviceRepository = deviceRepo)

            viewModel.onExportData()
            advanceUntilIdle()

            val data = viewModel.exportData.first { it != null }
            assertNotNull(data)
            assertTrue(data.isNotEmpty())
        }

    @Test
    fun `onExportDataConsumed clears exportData`() =
        runTest {
            val deviceRepo = FakeDeviceRepository()
            deviceRepo.setDevices(listOf(TestDevices.createDevice(id = "d1")))
            val viewModel = createViewModel(deviceRepository = deviceRepo)

            viewModel.onExportData()
            advanceUntilIdle()
            assertNotNull(viewModel.exportData.value)

            viewModel.onExportDataConsumed()

            assertNull(viewModel.exportData.value)
        }

    // endregion

    // region AI engine tests

    @Test
    fun `aiEngineType reflects repository state`() =
        runTest {
            val aiRepo = FakeAiPreferencesRepository()
            val viewModel = createViewModel(aiPreferencesRepository = aiRepo)
            advanceUntilIdle()

            val type = viewModel.aiEngineType.first()

            assertEquals(AiEngineType.Cloud, type)
        }

    @Test
    fun `onAiEngineSelected updates repository`() =
        runTest {
            val aiRepo = FakeAiPreferencesRepository()
            val viewModel = createViewModel(aiPreferencesRepository = aiRepo)

            viewModel.onAiEngineSelected(AiEngineType.OnDevice)
            advanceUntilIdle()

            assertEquals(1, aiRepo.setAiEngineTypeCallCount)
            val type = viewModel.aiEngineType.first { it == AiEngineType.OnDevice }
            assertEquals(AiEngineType.OnDevice, type)
        }

    // endregion

    // region Database recovery tests

    @Test
    fun `currentDatabaseFileName reflects data mode`() =
        runTest {
            val legacyRepo = FakeLegacyDatabaseRepository()
            legacyRepo.fileNameByMode[DataMode.None] = "battery-butler-offline.db"
            val viewModel = createViewModel(legacyDatabaseRepository = legacyRepo)
            advanceUntilIdle()

            val fileName = viewModel.currentDatabaseFileName.first { it.isNotEmpty() }

            assertEquals("battery-butler-offline.db", fileName)
        }

    @Test
    fun `legacyDatabaseInfo is null for modes without legacy files`() =
        runTest {
            val networkRepo = FakeDataModeRepository()
            networkRepo.setCurrentMode(DataMode.GrpcLocal("http://localhost:50051"))
            val legacyRepo = FakeLegacyDatabaseRepository()
            val viewModel = createViewModel(
                dataModeRepository = networkRepo,
                legacyDatabaseRepository = legacyRepo,
            )
            advanceUntilIdle()

            val info = viewModel.legacyDatabaseInfo.value

            assertNull(info)
        }

    @Test
    fun `legacyDatabaseInfo shows existing legacy file`() =
        runTest {
            val legacyRepo = FakeLegacyDatabaseRepository()
            legacyRepo.legacyInfoByMode[DataMode.None] = LegacyDatabaseInfo(
                legacyFileName = "battery-butler.db",
                exists = true,
            )
            val viewModel = createViewModel(legacyDatabaseRepository = legacyRepo)
            advanceUntilIdle()

            val info = viewModel.legacyDatabaseInfo.first { it != null }

            assertNotNull(info)
            assertEquals("battery-butler.db", info.legacyFileName)
            assertTrue(info.exists)
        }

    @Test
    fun `onRestoreLegacyDatabase calls use case with legacy file name`() =
        runTest {
            val legacyRepo = FakeLegacyDatabaseRepository()
            legacyRepo.legacyInfoByMode[DataMode.None] = LegacyDatabaseInfo(
                legacyFileName = "battery-butler.db",
                exists = true,
            )
            val viewModel = createViewModel(legacyDatabaseRepository = legacyRepo)
            advanceUntilIdle()
            // Wait for legacyDatabaseInfo to be populated
            viewModel.legacyDatabaseInfo.first { it != null }

            viewModel.onRestoreLegacyDatabase()
            advanceUntilIdle()

            assertEquals(1, legacyRepo.restoreCallCount)
            assertEquals("battery-butler.db", legacyRepo.lastRestoredFileName)
        }

    @Test
    fun `onRestoreLegacyDatabase sets restoreComplete on success`() =
        runTest {
            val legacyRepo = FakeLegacyDatabaseRepository()
            legacyRepo.legacyInfoByMode[DataMode.None] = LegacyDatabaseInfo(
                legacyFileName = "battery-butler.db",
                exists = true,
            )
            val viewModel = createViewModel(legacyDatabaseRepository = legacyRepo)
            advanceUntilIdle()
            viewModel.legacyDatabaseInfo.first { it != null }

            viewModel.onRestoreLegacyDatabase()
            advanceUntilIdle()

            assertTrue(viewModel.restoreComplete.value)
            assertFalse(viewModel.restoreInProgress.value)
        }

    @Test
    fun `onRestoreLegacyDatabase does nothing when no legacy info`() =
        runTest {
            val legacyRepo = FakeLegacyDatabaseRepository()
            // No legacy info configured — legacyDatabaseInfo will be null
            val viewModel = createViewModel(legacyDatabaseRepository = legacyRepo)
            advanceUntilIdle()

            viewModel.onRestoreLegacyDatabase()
            advanceUntilIdle()

            assertEquals(0, legacyRepo.restoreCallCount)
            assertFalse(viewModel.restoreComplete.value)
        }

    @Test
    fun `onRestoreCompleteAcknowledged resets restoreComplete`() =
        runTest {
            val legacyRepo = FakeLegacyDatabaseRepository()
            legacyRepo.legacyInfoByMode[DataMode.None] = LegacyDatabaseInfo(
                legacyFileName = "battery-butler.db",
                exists = true,
            )
            val viewModel = createViewModel(legacyDatabaseRepository = legacyRepo)
            advanceUntilIdle()
            viewModel.legacyDatabaseInfo.first { it != null }

            viewModel.onRestoreLegacyDatabase()
            advanceUntilIdle()
            assertTrue(viewModel.restoreComplete.value)

            viewModel.onRestoreCompleteAcknowledged()

            assertFalse(viewModel.restoreComplete.value)
            assertEquals(null, viewModel.restoreResult.value)
        }

    @Test
    fun `onRestoreLegacyDatabase exposes Success via restoreResult`() =
        runTest {
            val legacyRepo = FakeLegacyDatabaseRepository()
            legacyRepo.legacyInfoByMode[DataMode.None] = LegacyDatabaseInfo("battery-butler.db", true)
            legacyRepo.restoreResult = RestoreResult.Success
            val viewModel = createViewModel(legacyDatabaseRepository = legacyRepo)
            advanceUntilIdle()
            viewModel.legacyDatabaseInfo.first { it != null }

            viewModel.onRestoreLegacyDatabase()
            advanceUntilIdle()

            assertEquals(RestoreResult.Success, viewModel.restoreResult.value)
            assertTrue(viewModel.restoreComplete.value)
        }

    @Test
    fun `onRestoreLegacyDatabase exposes DestructiveFallback and still flags complete`() =
        runTest {
            val legacyRepo = FakeLegacyDatabaseRepository()
            legacyRepo.legacyInfoByMode[DataMode.None] = LegacyDatabaseInfo("battery-butler.db", true)
            legacyRepo.restoreResult = RestoreResult.DestructiveFallback(fromVersion = 0)
            val viewModel = createViewModel(legacyDatabaseRepository = legacyRepo)
            advanceUntilIdle()
            viewModel.legacyDatabaseInfo.first { it != null }

            viewModel.onRestoreLegacyDatabase()
            advanceUntilIdle()

            val result = viewModel.restoreResult.value
            assertTrue(result is RestoreResult.DestructiveFallback, "expected DestructiveFallback, got $result")
            // Restore "completed" (DB is usable) — UI still dismisses the dialog,
            // but the snackbar should warn about data loss via restoreResult.
            assertTrue(viewModel.restoreComplete.value)
        }

    @Test
    fun `onRestoreLegacyDatabase exposes Failure without flagging complete`() =
        runTest {
            val legacyRepo = FakeLegacyDatabaseRepository()
            legacyRepo.legacyInfoByMode[DataMode.None] = LegacyDatabaseInfo("battery-butler.db", true)
            legacyRepo.restoreResult = RestoreResult.Failure(
                errorMessage = "copy failed",
                throwableClassName = "IOException",
            )
            val viewModel = createViewModel(legacyDatabaseRepository = legacyRepo)
            advanceUntilIdle()
            viewModel.legacyDatabaseInfo.first { it != null }

            viewModel.onRestoreLegacyDatabase()
            advanceUntilIdle()

            val result = viewModel.restoreResult.value
            assertTrue(result is RestoreResult.Failure)
            assertFalse(viewModel.restoreComplete.value, "Failure must not flag restoreComplete")
        }

    @Test
    fun `onRestoreLegacyDatabase exposes LegacyFileUnavailable without flagging complete`() =
        runTest {
            val legacyRepo = FakeLegacyDatabaseRepository()
            legacyRepo.legacyInfoByMode[DataMode.None] = LegacyDatabaseInfo("battery-butler.db", true)
            legacyRepo.restoreResult = RestoreResult.LegacyFileUnavailable(reason = "file not found")
            val viewModel = createViewModel(legacyDatabaseRepository = legacyRepo)
            advanceUntilIdle()
            viewModel.legacyDatabaseInfo.first { it != null }

            viewModel.onRestoreLegacyDatabase()
            advanceUntilIdle()

            val result = viewModel.restoreResult.value
            assertTrue(result is RestoreResult.LegacyFileUnavailable)
            assertFalse(viewModel.restoreComplete.value)
        }

    @Test
    fun `onRestoreLegacyDatabase requests restart on Success`() =
        runTest {
            val legacyRepo = FakeLegacyDatabaseRepository()
            legacyRepo.legacyInfoByMode[DataMode.None] = LegacyDatabaseInfo("battery-butler.db", true)
            legacyRepo.restoreResult = RestoreResult.Success
            val coordinator = RestartCoordinator()
            val events = mutableListOf<Unit>()
            val collector = launch(testDispatcher) {
                coordinator.events.collect { events.add(it) }
            }
            val viewModel = createViewModel(
                legacyDatabaseRepository = legacyRepo,
                restartCoordinator = coordinator,
            )
            advanceUntilIdle()
            viewModel.legacyDatabaseInfo.first { it != null }

            viewModel.onRestoreLegacyDatabase()
            advanceUntilIdle()

            assertEquals(1, events.size, "expected exactly one restart request for Success")
            collector.cancel()
        }

    @Test
    fun `onRestoreLegacyDatabase requests restart on DestructiveFallback`() =
        runTest {
            val legacyRepo = FakeLegacyDatabaseRepository()
            legacyRepo.legacyInfoByMode[DataMode.None] = LegacyDatabaseInfo("battery-butler.db", true)
            legacyRepo.restoreResult = RestoreResult.DestructiveFallback(fromVersion = 0)
            val coordinator = RestartCoordinator()
            val events = mutableListOf<Unit>()
            val collector = launch(testDispatcher) {
                coordinator.events.collect { events.add(it) }
            }
            val viewModel = createViewModel(
                legacyDatabaseRepository = legacyRepo,
                restartCoordinator = coordinator,
            )
            advanceUntilIdle()
            viewModel.legacyDatabaseInfo.first { it != null }

            viewModel.onRestoreLegacyDatabase()
            advanceUntilIdle()

            assertEquals(1, events.size, "expected exactly one restart request for DestructiveFallback")
            collector.cancel()
        }

    @Test
    fun `onRestoreLegacyDatabase does NOT request restart on Failure`() =
        runTest {
            val legacyRepo = FakeLegacyDatabaseRepository()
            legacyRepo.legacyInfoByMode[DataMode.None] = LegacyDatabaseInfo("battery-butler.db", true)
            legacyRepo.restoreResult = RestoreResult.Failure("copy failed", "IOException")
            val coordinator = RestartCoordinator()
            val events = mutableListOf<Unit>()
            val collector = launch(testDispatcher) {
                coordinator.events.collect { events.add(it) }
            }
            val viewModel = createViewModel(
                legacyDatabaseRepository = legacyRepo,
                restartCoordinator = coordinator,
            )
            advanceUntilIdle()
            viewModel.legacyDatabaseInfo.first { it != null }

            viewModel.onRestoreLegacyDatabase()
            advanceUntilIdle()

            assertEquals(0, events.size, "Failure must not request restart")
            collector.cancel()
        }

    // endregion

    private fun createViewModel(
        deviceRepository: FakeDeviceRepository = FakeDeviceRepository(),
        dataModeRepository: FakeDataModeRepository = FakeDataModeRepository(),
        appInfoRepository: AppInfoRepository = FakeAppInfoRepository(),
        authRepository: FakeAuthRepository = FakeAuthRepository(),
        labsAuthRepository: FakeLabsAuthRepository = FakeLabsAuthRepository(),
        aiPreferencesRepository: FakeAiPreferencesRepository = FakeAiPreferencesRepository(),
        legacyDatabaseRepository: FakeLegacyDatabaseRepository = FakeLegacyDatabaseRepository(),
        restartCoordinator: RestartCoordinator = RestartCoordinator(),
        featureFlagProvider: FeatureFlagProvider = FakeFeatureFlagProvider(),
    ): SettingsViewModel =
        SettingsViewModel(
            exportDataUseCase = ExportDataUseCase(deviceRepository, testDispatcherProvider),
            importDataUseCase = ImportDataUseCase(deviceRepository, testDispatcherProvider),
            dataModeRepository = dataModeRepository,
            getAppVersionUseCase = GetAppVersionUseCase(appInfoRepository),
            authRepository = authRepository,
            labsAuthRepository = labsAuthRepository,
            aiPreferencesRepository = aiPreferencesRepository,
            getLegacyDatabaseInfoUseCase = GetLegacyDatabaseInfoUseCase(legacyDatabaseRepository),
            restoreLegacyDatabaseUseCase = RestoreLegacyDatabaseUseCase(legacyDatabaseRepository),
            legacyDatabaseRepository = legacyDatabaseRepository,
            restartCoordinator = restartCoordinator,
            signInToLabsUseCase = SignInToLabsUseCase(labsAuthRepository, deviceRepository, useCaseScope),
            signOutLabsUseCase = SignOutLabsUseCase(labsAuthRepository, deviceRepository),
            signOutUseCase = SignOutUseCase(authRepository, deviceRepository),
            featureFlagProvider = featureFlagProvider,
            productionServerUrl = ProductionServerUrl("http://test-server:80"),
            devServerUrl = DevServerUrl("http://test-dev-server:80"),
            labsStagingUrl = LabsStagingUrl("https://staging.example"),
            labsProdUrl = LabsProdUrl("https://prod.example"),
        )

    // region Data mode availability tests

    @Test
    fun `availableDataModes hides legacy modes and orders Device only, Production, Staging, Mock by default`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(
                listOf(
                    DataMode.None,
                    DataMode.LabsProd("https://prod.example"),
                    DataMode.LabsStaging("https://staging.example"),
                    DataMode.Mock,
                ),
                viewModel.availableDataModes,
            )
        }

    @Test
    fun `availableDataModes includes legacy modes when LEGACY_DATA_MODES flag is enabled`() =
        runTest {
            val viewModel = createViewModel(
                featureFlagProvider = FakeFeatureFlagProvider(setOf(FeatureFlag.LEGACY_DATA_MODES)),
            )
            advanceUntilIdle()

            assertEquals(7, viewModel.availableDataModes.size)
            assertTrue(viewModel.availableDataModes.any { it is DataMode.GrpcLocal })
            assertTrue(viewModel.availableDataModes.any { it is DataMode.GrpcAws })
            assertTrue(viewModel.availableDataModes.any { it is DataMode.GrpcDev })
        }

    // endregion
}
