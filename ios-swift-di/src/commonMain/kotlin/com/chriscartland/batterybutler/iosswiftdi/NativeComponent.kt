package com.chriscartland.batterybutler.iosswiftdi

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.chriscartland.batterybutler.ai.NoOpAiEngine
import com.chriscartland.batterybutler.data.di.BaseDataComponent
import com.chriscartland.batterybutler.data.repository.DataStoreNetworkModeRepository
import com.chriscartland.batterybutler.data.repository.DefaultDeviceRepository
import com.chriscartland.batterybutler.data.repository.DefaultFeatureFlagProvider
import com.chriscartland.batterybutler.datalocal.LocalDataSource
import com.chriscartland.batterybutler.datalocal.RoomLocalDataSource
import com.chriscartland.batterybutler.datalocal.preferences.DataStoreFactory
import com.chriscartland.batterybutler.datalocal.preferences.DataStorePreferencesDataSource
import com.chriscartland.batterybutler.datalocal.preferences.PreferencesDataSource
import com.chriscartland.batterybutler.datalocal.room.AppDatabase
import com.chriscartland.batterybutler.datalocal.room.DatabaseFactory
import com.chriscartland.batterybutler.datalocal.room.DeviceDao
import com.chriscartland.batterybutler.datanetwork.RemoteDataSource
import com.chriscartland.batterybutler.domain.model.FeatureFlag
import com.chriscartland.batterybutler.domain.repository.AiEngine
import com.chriscartland.batterybutler.domain.repository.AuthRepository
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.domain.repository.FeatureFlagProvider
import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import com.chriscartland.batterybutler.usecase.di.UseCaseComponent
import com.chriscartland.batterybutler.viewmodel.adddevice.AddDeviceViewModel
import com.chriscartland.batterybutler.viewmodel.adddevicetype.AddDeviceTypeViewModel
import com.chriscartland.batterybutler.viewmodel.devicedetail.DeviceDetailViewModelFactory
import com.chriscartland.batterybutler.viewmodel.devicetypes.DeviceTypeListViewModel
import com.chriscartland.batterybutler.viewmodel.devicetypes.EditDeviceTypeViewModelFactory
import com.chriscartland.batterybutler.viewmodel.history.HistoryListViewModel
import com.chriscartland.batterybutler.viewmodel.home.HomeViewModel
import com.chriscartland.batterybutler.viewmodel.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope

@Scope
annotation class SharedSingleton

@Component
@SharedSingleton
abstract class NativeComponent(
    @get:Provides override val databaseFactory: DatabaseFactory,
    @get:Provides override val dataStoreFactory: DataStoreFactory,
    @get:Provides val aiEngine: AiEngine,
    @get:Provides val remoteDataSource: RemoteDataSource,
    @get:Provides val authRepository: AuthRepository,
) : UseCaseComponent(),
    BaseDataComponent {
    abstract val homeViewModel: HomeViewModel
    abstract val addDeviceViewModel: AddDeviceViewModel
    abstract val settingsViewModel: SettingsViewModel
    abstract val historyListViewModel: HistoryListViewModel
    abstract val deviceTypeListViewModel: DeviceTypeListViewModel
    abstract val deviceDetailViewModelFactory: DeviceDetailViewModelFactory
    abstract val addDeviceTypeViewModel: AddDeviceTypeViewModel
    abstract val editDeviceTypeViewModelFactory: EditDeviceTypeViewModelFactory

    // BaseDataComponent defines 'val appScope: CoroutineScope'.
    // kotlin-inject will implement the abstract property using the provider.
    abstract override val appScope: CoroutineScope

    @Provides
    @SharedSingleton
    fun provideAppScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @SharedSingleton
    override fun provideAppDatabase(): AppDatabase = super.provideAppDatabase()

    @Provides
    @SharedSingleton
    override fun provideDeviceDao(database: AppDatabase): DeviceDao = super.provideDeviceDao(database)

    @Provides
    @SharedSingleton
    override fun providePreferencesDataStore(): DataStore<Preferences> = super.providePreferencesDataStore()

    @Provides
    @SharedSingleton
    override fun providePreferencesDataSource(dataSource: DataStorePreferencesDataSource): PreferencesDataSource = super.providePreferencesDataSource(dataSource)

    @Provides
    @SharedSingleton
    override fun provideNetworkModeRepository(repo: DataStoreNetworkModeRepository): NetworkModeRepository = super.provideNetworkModeRepository(repo)

    @Provides
    @SharedSingleton
    // We override to add Scope, but implementation comes from Base
    override fun provideDeviceRepository(repo: DefaultDeviceRepository): DeviceRepository = super.provideDeviceRepository(repo)

    @Provides
    @SharedSingleton
    override fun provideLocalDataSource(dataSource: RoomLocalDataSource): LocalDataSource = super.provideLocalDataSource(dataSource)

    // RemoteDataSource and AuthRepository are provided by constructor vals (passed from Swift)
    // We don't need to do anything as `BaseDataComponent` doesn't provide them (we removed them).
    // And `DefaultDeviceRepository` injection in `provideDeviceRepository` will pick up `RemoteDataSource` from the graph.
    // The graph has `val remoteDataSource` (from constructor).
    // So it should work!

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
    fun provideAppVersion(): com.chriscartland.batterybutler.domain.model.AppVersion =
        com.chriscartland.batterybutler.domain.model.AppVersion
            .Ios(versionName = "iOS Native", buildNumber = "1")

    @Provides
    @SharedSingleton
    fun provideAppInfoRepository(
        impl: com.chriscartland.batterybutler.data.repository.StaticAppInfoRepository,
    ): com.chriscartland.batterybutler.domain.repository.AppInfoRepository = impl
}
