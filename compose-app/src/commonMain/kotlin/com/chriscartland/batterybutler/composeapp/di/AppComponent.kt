package com.chriscartland.batterybutler.composeapp.di

import com.chriscartland.batterybutler.ai.AiEngine
import com.chriscartland.batterybutler.composeapp.feature.ai.NoOpAiEngine
import com.chriscartland.batterybutler.data.repository.DefaultFeatureFlagProvider
import com.chriscartland.batterybutler.data.repository.StaticAppInfoRepository
import com.chriscartland.batterybutler.datalocal.room.DatabaseFactory
import com.chriscartland.batterybutler.datanetwork.grpc.DelegatingGrpcClient
import com.chriscartland.batterybutler.datanetwork.grpc.NetworkComponent
import com.chriscartland.batterybutler.domain.model.AppVersion
import com.chriscartland.batterybutler.domain.model.FeatureFlag
import com.chriscartland.batterybutler.domain.repository.AppInfoRepository
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.domain.repository.FeatureFlagProvider
import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import com.chriscartland.batterybutler.usecase.di.UseCaseComponent
import com.chriscartland.batterybutler.viewmodel.addbatteryevent.AddBatteryEventViewModel
import com.chriscartland.batterybutler.viewmodel.adddevice.AddDeviceViewModel
import com.chriscartland.batterybutler.viewmodel.adddevicetype.AddDeviceTypeViewModel
import com.chriscartland.batterybutler.viewmodel.devicedetail.DeviceDetailViewModelFactory
import com.chriscartland.batterybutler.viewmodel.devicetypes.DeviceTypeListViewModel
import com.chriscartland.batterybutler.viewmodel.devicetypes.EditDeviceTypeViewModelFactory
import com.chriscartland.batterybutler.viewmodel.editdevice.EditDeviceViewModelFactory
import com.chriscartland.batterybutler.viewmodel.eventdetail.EventDetailViewModelFactory
import com.chriscartland.batterybutler.viewmodel.history.HistoryListViewModel
import com.chriscartland.batterybutler.viewmodel.home.HomeViewModel
import com.chriscartland.batterybutler.viewmodel.settings.SettingsViewModel
import com.squareup.wire.GrpcClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

@Component
@Singleton
abstract class AppComponent(
    // We pass this through to DataComponent via AppDataModule
    override val databaseFactory: DatabaseFactory,
    @get:Provides val aiEngine: AiEngine,
    override val networkComponent: NetworkComponent,
    @get:Provides val appVersion: AppVersion,
) : UseCaseComponent(),
    AppDataModule {
    abstract val homeViewModel: HomeViewModel
    abstract val addDeviceViewModel: AddDeviceViewModel
    abstract val addDeviceTypeViewModel: AddDeviceTypeViewModel
    abstract val addBatteryEventViewModel: AddBatteryEventViewModel
    abstract val historyListViewModel: HistoryListViewModel
    abstract val deviceDetailViewModelFactory: DeviceDetailViewModelFactory
    abstract val editDeviceViewModelFactory: EditDeviceViewModelFactory
    abstract val eventDetailViewModelFactory: EventDetailViewModelFactory
    abstract val deviceTypeListViewModel: DeviceTypeListViewModel
    abstract val editDeviceTypeViewModelFactory: EditDeviceTypeViewModelFactory
    abstract val deviceRepository: DeviceRepository
    abstract val settingsViewModel: SettingsViewModel
    abstract val networkModeRepository: NetworkModeRepository
    abstract val appScope: CoroutineScope

    @Provides
    @Singleton
    fun provideAppInfoRepository(repo: StaticAppInfoRepository): AppInfoRepository = repo

    @Provides
    @Singleton
    fun provideFeatureFlagProvider(aiEngine: AiEngine): FeatureFlagProvider {
        val enabledFeatures = buildSet {
            // AI features are enabled when we have a real AI engine (not NoOp)
            if (aiEngine !is NoOpAiEngine) {
                add(FeatureFlag.AI_BATCH_IMPORT)
            }
            // Remote sync is always available (server might not be configured, but feature exists)
            add(FeatureFlag.REMOTE_SYNC)
        }
        return DefaultFeatureFlagProvider(enabledFeatures)
    }

    @Provides
    @Singleton
    fun provideAppScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    fun provideDatabaseFactory(): DatabaseFactory = databaseFactory

    @Provides
    @Singleton
    fun provideDelegatingGrpcClient(
        networkModeRepository: NetworkModeRepository,
        scope: CoroutineScope,
    ): DelegatingGrpcClient =
        DelegatingGrpcClient(
            factory = networkComponent::createGrpcClient,
            networkModeRepository = networkModeRepository,
            scope = scope,
        )

    @Provides
    @Singleton
    fun provideGrpcClient(impl: DelegatingGrpcClient): GrpcClient = impl

    companion object
}
