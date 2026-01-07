package com.chriscartland.batterybutler.di

import com.chriscartland.batterybutler.data.di.DatabaseFactory
import com.chriscartland.batterybutler.data.repository.RoomDeviceRepository
import com.chriscartland.batterybutler.data.room.AppDatabase
import com.chriscartland.batterybutler.data.room.DeviceDao
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.feature.adddevice.AddDeviceViewModel
import com.chriscartland.batterybutler.feature.adddevicetype.AddDeviceTypeViewModel
import com.chriscartland.batterybutler.feature.devicedetail.DeviceDetailViewModelFactory
import com.chriscartland.batterybutler.domain.ai.AiEngine
import com.chriscartland.batterybutler.feature.ai.AiViewModel
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
    private val databaseFactory: DatabaseFactory,
    @get:Provides val aiEngine: AiEngine,
) {
    abstract val homeViewModel: HomeViewModel
    abstract val aiViewModel: AiViewModel
    abstract val addDeviceViewModel: AddDeviceViewModel
    abstract val addDeviceTypeViewModel: AddDeviceTypeViewModel
    abstract val historyListViewModel: HistoryListViewModel
    abstract val deviceDetailViewModelFactory: DeviceDetailViewModelFactory
    abstract val editDeviceViewModelFactory: EditDeviceViewModelFactory
    abstract val eventDetailViewModelFactory: EventDetailViewModelFactory
    abstract val deviceTypeListViewModel: DeviceTypeListViewModel
    abstract val editDeviceTypeViewModelFactory: EditDeviceTypeViewModelFactory
    abstract val deviceRepository: DeviceRepository

    @Provides
    @Singleton
    fun provideAppDatabase(): AppDatabase = databaseFactory.createDatabase()

    @Provides
    fun provideDeviceDao(database: AppDatabase): DeviceDao = database.deviceDao()

    @Provides
    @Singleton
    fun provideDeviceRepository(repo: RoomDeviceRepository): DeviceRepository = repo
}
