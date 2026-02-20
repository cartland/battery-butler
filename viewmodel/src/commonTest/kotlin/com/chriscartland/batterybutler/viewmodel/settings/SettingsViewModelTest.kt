package com.chriscartland.batterybutler.viewmodel.settings

import com.chriscartland.batterybutler.domain.model.AppVersion
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.DispatcherProvider
import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.domain.model.ProductionServerUrl
import com.chriscartland.batterybutler.domain.model.User
import com.chriscartland.batterybutler.domain.model.ai.AiEngineType
import com.chriscartland.batterybutler.domain.repository.AiPreferencesRepository
import com.chriscartland.batterybutler.domain.repository.AppInfoRepository
import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import com.chriscartland.batterybutler.testcommon.FakeAuthRepository
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import com.chriscartland.batterybutler.usecase.ExportDataUseCase
import com.chriscartland.batterybutler.usecase.GetAppVersionUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region Fake implementations

    private class FakeNetworkModeRepository : NetworkModeRepository {
        private val _networkMode = MutableStateFlow<NetworkMode>(NetworkMode.None)
        override val networkMode: Flow<NetworkMode> = _networkMode
        var setNetworkModeCallCount = 0
            private set
        var lastSetMode: NetworkMode? = null
            private set

        override suspend fun setNetworkMode(mode: NetworkMode) {
            setNetworkModeCallCount++
            lastSetMode = mode
            _networkMode.value = mode
        }

        fun setCurrentMode(mode: NetworkMode) {
            _networkMode.value = mode
        }
    }

    private class FakeAppInfoRepository(
        private val version: AppVersion = AppVersion.Unavailable,
    ) : AppInfoRepository {
        override fun getAppVersion(): AppVersion = version
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
            authRepo.setAuthState(AuthState.Unauthenticated)
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
    fun `isSignedIn is false when unauthenticated`() =
        runTest {
            val authRepo = FakeAuthRepository()
            authRepo.setAuthState(AuthState.Unauthenticated)
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
    fun `signOut calls auth repository`() =
        runTest {
            val authRepo = FakeAuthRepository()
            val viewModel = createViewModel(authRepository = authRepo)

            viewModel.signOut()
            advanceUntilIdle()

            assertEquals(1, authRepo.signOutCallCount)
        }

    // endregion

    // region Network mode tests

    @Test
    fun `networkMode reflects repository state`() =
        runTest {
            val networkRepo = FakeNetworkModeRepository()
            networkRepo.setCurrentMode(NetworkMode.Mock)
            val viewModel = createViewModel(networkModeRepository = networkRepo)
            advanceUntilIdle()

            val mode = viewModel.networkMode.first { it == NetworkMode.Mock }

            assertEquals(NetworkMode.Mock, mode)
        }

    @Test
    fun `onNetworkModeSelected updates repository`() =
        runTest {
            val networkRepo = FakeNetworkModeRepository()
            val viewModel = createViewModel(networkModeRepository = networkRepo)

            viewModel.onNetworkModeSelected(NetworkMode.Mock)
            advanceUntilIdle()

            assertEquals(1, networkRepo.setNetworkModeCallCount)
            assertEquals(NetworkMode.Mock, networkRepo.lastSetMode)
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

    private fun createViewModel(
        deviceRepository: FakeDeviceRepository = FakeDeviceRepository(),
        networkModeRepository: FakeNetworkModeRepository = FakeNetworkModeRepository(),
        appInfoRepository: AppInfoRepository = FakeAppInfoRepository(),
        authRepository: FakeAuthRepository = FakeAuthRepository(),
        aiPreferencesRepository: FakeAiPreferencesRepository = FakeAiPreferencesRepository(),
    ): SettingsViewModel =
        SettingsViewModel(
            exportDataUseCase = ExportDataUseCase(deviceRepository, testDispatcherProvider),
            networkModeRepository = networkModeRepository,
            getAppVersionUseCase = GetAppVersionUseCase(appInfoRepository),
            authRepository = authRepository,
            aiPreferencesRepository = aiPreferencesRepository,
            productionServerUrl = ProductionServerUrl("http://test-server:80"),
        )
}
