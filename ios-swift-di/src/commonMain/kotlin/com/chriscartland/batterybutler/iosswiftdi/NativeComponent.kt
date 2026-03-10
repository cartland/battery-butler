package com.chriscartland.batterybutler.iosswiftdi

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.chriscartland.batterybutler.ai.NoOpAiEngine
import com.chriscartland.batterybutler.data.provider.DefaultDispatcherProvider
import com.chriscartland.batterybutler.data.repository.DataStoreNetworkModeRepository
import com.chriscartland.batterybutler.data.repository.DefaultDeviceRepository
import com.chriscartland.batterybutler.data.repository.DefaultFeatureFlagProvider
import com.chriscartland.batterybutler.data.repository.DefaultSyncManager
import com.chriscartland.batterybutler.data.repository.InMemoryAiPreferencesRepository
import com.chriscartland.batterybutler.data.repository.SyncManager
import com.chriscartland.batterybutler.datalocal.LocalDataSource
import com.chriscartland.batterybutler.datalocal.RoomLocalDataSource
import com.chriscartland.batterybutler.datalocal.preferences.DataStoreFactory
import com.chriscartland.batterybutler.datalocal.preferences.DataStorePreferencesDataSource
import com.chriscartland.batterybutler.datalocal.preferences.PreferencesDataSource
import com.chriscartland.batterybutler.datalocal.room.AppDatabase
import com.chriscartland.batterybutler.datalocal.room.DatabaseFactory
import com.chriscartland.batterybutler.datalocal.room.DeviceDao
import com.chriscartland.batterybutler.datanetwork.BuildConfig
import com.chriscartland.batterybutler.datanetwork.RemoteDataSource
import com.chriscartland.batterybutler.domain.model.DevServerUrl
import com.chriscartland.batterybutler.domain.model.DispatcherProvider
import com.chriscartland.batterybutler.domain.model.FeatureFlag
import com.chriscartland.batterybutler.domain.model.ProductionServerUrl
import com.chriscartland.batterybutler.domain.model.ai.AiEngine
import com.chriscartland.batterybutler.domain.repository.AiPreferencesRepository
import com.chriscartland.batterybutler.domain.repository.AuthRepository
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.domain.repository.FeatureFlagProvider
import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import com.chriscartland.batterybutler.viewmodel.addbatteryevent.AddBatteryEventViewModel
import com.chriscartland.batterybutler.viewmodel.adddevice.AddDeviceViewModel
import com.chriscartland.batterybutler.viewmodel.adddevicetype.AddDeviceTypeViewModel
import com.chriscartland.batterybutler.viewmodel.aichat.AiChatViewModel
import com.chriscartland.batterybutler.viewmodel.devicedetail.DeviceDetailViewModelFactory
import com.chriscartland.batterybutler.viewmodel.devicetypes.DeviceTypeDetailViewModelFactory
import com.chriscartland.batterybutler.viewmodel.devicetypes.DeviceTypeListViewModel
import com.chriscartland.batterybutler.viewmodel.devicetypes.EditDeviceTypeViewModelFactory
import com.chriscartland.batterybutler.viewmodel.editdevice.EditDeviceViewModelFactory
import com.chriscartland.batterybutler.viewmodel.eventdetail.EditBatteryEventViewModelFactory
import com.chriscartland.batterybutler.viewmodel.eventdetail.EventDetailViewModelFactory
import com.chriscartland.batterybutler.viewmodel.history.HistoryListViewModel
import com.chriscartland.batterybutler.viewmodel.home.HomeViewModel
import com.chriscartland.batterybutler.viewmodel.login.LoginViewModel
import com.chriscartland.batterybutler.viewmodel.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope

@Scope
annotation class SharedSingleton

@Component
@SharedSingleton
abstract class NativeComponent(
    @get:Provides val databaseFactory: DatabaseFactory,
    @get:Provides val dataStoreFactory: DataStoreFactory,
    @get:Provides val aiEngine: AiEngine,
    @get:Provides val remoteDataSource: RemoteDataSource,
    @get:Provides val authRepository: AuthRepository,
) {
    abstract val homeViewModel: HomeViewModel
    abstract val addDeviceViewModel: AddDeviceViewModel
    abstract val settingsViewModel: SettingsViewModel
    abstract val historyListViewModel: HistoryListViewModel
    abstract val deviceTypeListViewModel: DeviceTypeListViewModel
    abstract val deviceDetailViewModelFactory: DeviceDetailViewModelFactory
    abstract val addDeviceTypeViewModel: AddDeviceTypeViewModel
    abstract val deviceTypeDetailViewModelFactory: DeviceTypeDetailViewModelFactory
    abstract val editDeviceTypeViewModelFactory: EditDeviceTypeViewModelFactory
    abstract val editDeviceViewModelFactory: EditDeviceViewModelFactory
    abstract val editBatteryEventViewModelFactory: EditBatteryEventViewModelFactory
    abstract val aiChatViewModel: AiChatViewModel
    abstract val addBatteryEventViewModel: AddBatteryEventViewModel
    abstract val eventDetailViewModelFactory: EventDetailViewModelFactory
    abstract val loginViewModel: LoginViewModel

    // Can add other ViewModels as needed for the native app

    @Provides
    @SharedSingleton
    fun provideAppDatabase(): AppDatabase = databaseFactory.createDatabase()

    @Provides
    fun provideDeviceDao(database: AppDatabase): DeviceDao = database.deviceDao()

    @Provides
    @SharedSingleton
    fun provideLocalDataSource(dataSource: RoomLocalDataSource): LocalDataSource = dataSource

    @Provides
    @SharedSingleton
    fun provideSyncManager(manager: DefaultSyncManager): SyncManager = manager

    @Provides
    @SharedSingleton
    fun provideDeviceRepository(repo: DefaultDeviceRepository): DeviceRepository = repo

    @Provides
    @SharedSingleton
    fun providePreferencesDataStore(): DataStore<Preferences> = dataStoreFactory.createPreferencesDataStore()

    @Provides
    @SharedSingleton
    fun providePreferencesDataSource(dataSource: DataStorePreferencesDataSource): PreferencesDataSource = dataSource

    @Provides
    @SharedSingleton
    fun provideNetworkModeRepository(impl: DataStoreNetworkModeRepository): NetworkModeRepository = impl

    @Provides
    @SharedSingleton
    fun provideAppScope(dispatcherProvider: DispatcherProvider): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcherProvider.default)

    @Provides
    @SharedSingleton
    fun provideFeatureFlagProvider(): FeatureFlagProvider {
        val enabledFeatures = buildSet {
            if (aiEngine !is NoOpAiEngine) {
                add(FeatureFlag.AI_BATCH_IMPORT)
            }
            add(FeatureFlag.REMOTE_SYNC)
        }
        return DefaultFeatureFlagProvider(enabledFeatures)
    }

    @Provides
    @SharedSingleton
    fun provideProductionServerUrl(): ProductionServerUrl = ProductionServerUrl(BuildConfig.PRODUCTION_SERVER_URL)

    @Provides
    @SharedSingleton
    fun provideDevServerUrl(): DevServerUrl = DevServerUrl(BuildConfig.DEV_SERVER_URL)

    @Provides
    fun provideAppVersion(): com.chriscartland.batterybutler.domain.model.AppVersion {
        val bundle = platform.Foundation.NSBundle.mainBundle
        val version = bundle.infoDictionary?.get("CFBundleShortVersionString") as? String ?: "Unknown"
        val build = bundle.infoDictionary?.get("CFBundleVersion") as? String ?: "0"
        return com.chriscartland.batterybutler.domain.model.AppVersion
            .Ios(versionName = version, buildNumber = build)
    }

    @Provides
    @SharedSingleton
    fun provideAppInfoRepository(
        impl: com.chriscartland.batterybutler.data.repository.StaticAppInfoRepository,
    ): com.chriscartland.batterybutler.domain.repository.AppInfoRepository = impl

    @Provides
    fun provideDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider = impl

    @Provides
    @SharedSingleton
    fun provideAiPreferencesRepository(): AiPreferencesRepository = InMemoryAiPreferencesRepository()
}
