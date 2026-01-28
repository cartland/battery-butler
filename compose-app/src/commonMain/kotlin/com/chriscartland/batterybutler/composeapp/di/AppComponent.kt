package com.chriscartland.batterybutler.composeapp.di

import com.chriscartland.batterybutler.ai.AiEngine
import com.chriscartland.batterybutler.data.di.DataComponent
import com.chriscartland.batterybutler.data.repository.DefaultDeviceRepository
import com.chriscartland.batterybutler.data.repository.InMemoryNetworkModeRepository
import com.chriscartland.batterybutler.data.repository.StaticAppInfoRepository
import com.chriscartland.batterybutler.datalocal.room.AppDatabase
import com.chriscartland.batterybutler.datalocal.room.DatabaseFactory
import com.chriscartland.batterybutler.datanetwork.DelegatingRemoteDataSource
import com.chriscartland.batterybutler.datanetwork.RemoteDataSource
import com.chriscartland.batterybutler.datanetwork.grpc.DelegatingGrpcClient
import com.chriscartland.batterybutler.datanetwork.grpc.NetworkComponent
import com.chriscartland.batterybutler.domain.model.AppVersion
import com.chriscartland.batterybutler.domain.repository.AppInfoRepository
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import com.chriscartland.batterybutler.usecase.GetAppVersionUseCase
import com.chriscartland.batterybutler.usecase.SetNetworkModeUseCase
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
import me.tatarka.inject.annotations.Scope

@Scope
annotation class Singleton

@Component
@Singleton
abstract class AppComponent(
    // We pass this through to DataComponent
    override val databaseFactory: DatabaseFactory,
    @get:Provides val aiEngine: AiEngine,
    override val networkComponent: NetworkComponent,
    @get:Provides val appVersion: AppVersion,
) : UseCaseComponent(),
    DataComponent {
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
    abstract override val setNetworkModeUseCase: SetNetworkModeUseCase
    abstract override val getAppVersionUseCase: GetAppVersionUseCase
    abstract val appScope: CoroutineScope

    @Provides
    @Singleton
    override fun provideAppDatabase(): AppDatabase = super.provideAppDatabase()

    @Provides
    @Singleton
    override fun provideNetworkModeRepository(repo: InMemoryNetworkModeRepository): NetworkModeRepository = super.provideNetworkModeRepository(repo)

    @Provides
    @Singleton
    override fun provideRemoteDataSource(dataSource: DelegatingRemoteDataSource): RemoteDataSource = super.provideRemoteDataSource(dataSource)

    @Provides
    @Singleton
    override fun provideDeviceRepository(repo: DefaultDeviceRepository): DeviceRepository = super.provideDeviceRepository(repo)

    @Provides
    @Singleton
    fun provideAppInfoRepository(repo: StaticAppInfoRepository): AppInfoRepository = repo

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
