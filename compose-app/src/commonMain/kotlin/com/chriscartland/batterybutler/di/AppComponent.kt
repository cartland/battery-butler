package com.chriscartland.batterybutler.di

import com.chriscartland.batterybutler.data.di.DataComponent
import com.chriscartland.batterybutler.data.di.DatabaseFactory
import com.chriscartland.batterybutler.data.repository.RoomDeviceRepository
import com.chriscartland.batterybutler.data.room.AppDatabase
import com.chriscartland.batterybutler.domain.ai.AiEngine
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.feature.addbatteryevent.AddBatteryEventViewModel
import com.chriscartland.batterybutler.feature.adddevice.AddDeviceViewModel
import com.chriscartland.batterybutler.feature.adddevicetype.AddDeviceTypeViewModel
import com.chriscartland.batterybutler.feature.devicedetail.DeviceDetailViewModelFactory
import com.chriscartland.batterybutler.feature.devicetypes.DeviceTypeListViewModel
import com.chriscartland.batterybutler.feature.devicetypes.EditDeviceTypeViewModelFactory
import com.chriscartland.batterybutler.feature.editdevice.EditDeviceViewModelFactory
import com.chriscartland.batterybutler.feature.eventdetail.EventDetailViewModelFactory
import com.chriscartland.batterybutler.feature.history.HistoryListViewModel
import com.chriscartland.batterybutler.feature.home.HomeViewModel
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
) : DataComponent {
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

    // DataComponent provides AppDatabase, DeviceDao, and DeviceRepository
    // We need to ensure that provideAppDatabase and provideDeviceRepository are scoped to Singleton.
    // Since they are defined in the interface, we may need to override them to add the scope annotation,
    // or we assume they are lightweight/stateless enough, OR we check if kotlin-inject supports scope on interface implementation.
    //
    // However, DataComponent methods are just default implementations. 
    // To enforcing Singleton scope on the Database instance:
    
    @Provides
    @Singleton
    override fun provideAppDatabase(): AppDatabase = super.provideAppDatabase()

    @Provides
    @Singleton
    override fun provideDeviceRepository(repo: RoomDeviceRepository): DeviceRepository = super.provideDeviceRepository(repo)
}
