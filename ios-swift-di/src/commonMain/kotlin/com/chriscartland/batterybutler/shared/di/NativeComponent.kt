package com.chriscartland.batterybutler.shared.di

import com.chriscartland.batterybutler.data.di.DatabaseFactory
import com.chriscartland.batterybutler.data.repository.RoomDeviceRepository
import com.chriscartland.batterybutler.data.room.AppDatabase
import com.chriscartland.batterybutler.data.room.DeviceDao
import com.chriscartland.batterybutler.domain.ai.AiEngine
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.domain.repository.RemoteDataSource
import com.chriscartland.batterybutler.viewmodel.adddevice.AddDeviceViewModel
import com.chriscartland.batterybutler.viewmodel.devicedetail.DeviceDetailViewModelFactory
import com.chriscartland.batterybutler.viewmodel.devicetypes.DeviceTypeListViewModel
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
    private val databaseFactory: DatabaseFactory,
    @get:Provides val aiEngine: AiEngine,
    @get:Provides val remoteDataSource: RemoteDataSource,
) {
    abstract val homeViewModel: HomeViewModel
    abstract val addDeviceViewModel: AddDeviceViewModel
    abstract val settingsViewModel: SettingsViewModel
    abstract val historyListViewModel: HistoryListViewModel
    abstract val deviceTypeListViewModel: DeviceTypeListViewModel
    abstract val deviceDetailViewModelFactory: DeviceDetailViewModelFactory

    // Can add other ViewModels as needed for the native app

    @Provides
    @SharedSingleton
    fun provideAppDatabase(): AppDatabase = databaseFactory.createDatabase()

    @Provides
    fun provideDeviceDao(database: AppDatabase): DeviceDao = database.deviceDao()

    @Provides
    @SharedSingleton
    fun provideDeviceRepository(repo: RoomDeviceRepository): DeviceRepository = repo

    @Provides
    @SharedSingleton
    fun provideAppScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
