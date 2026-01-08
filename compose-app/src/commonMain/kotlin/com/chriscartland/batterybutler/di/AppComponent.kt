package com.chriscartland.batterybutler.di

import com.chriscartland.batterybutler.data.di.DataComponent
import com.chriscartland.batterybutler.data.di.DatabaseFactory
import com.chriscartland.batterybutler.data.repository.RoomDeviceRepository
import com.chriscartland.batterybutler.data.room.AppDatabase
import com.chriscartland.batterybutler.domain.ai.AiEngine
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
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
import com.chriscartland.batterybutler.domain.repository.RemoteDataSource
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
    @get:Provides val remoteDataSource: RemoteDataSource,
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

    @Provides
    @Singleton
    override fun provideAppDatabase(): AppDatabase = super.provideAppDatabase()

    @Provides
    @Singleton
    override fun provideDeviceRepository(repo: RoomDeviceRepository): DeviceRepository = super.provideDeviceRepository(repo)

    @Provides
    @Singleton
    fun provideAppScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
