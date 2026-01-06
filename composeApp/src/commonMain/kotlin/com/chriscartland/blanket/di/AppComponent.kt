package com.chriscartland.blanket.di

import com.chriscartland.blanket.data.di.DatabaseFactory
import com.chriscartland.blanket.data.repository.RoomDeviceRepository
import com.chriscartland.blanket.data.room.AppDatabase
import com.chriscartland.blanket.data.room.DeviceDao
import com.chriscartland.blanket.domain.repository.DeviceRepository
import com.chriscartland.blanket.feature.adddevice.AddDeviceViewModel
import com.chriscartland.blanket.feature.adddevicetype.AddDeviceTypeViewModel
import com.chriscartland.blanket.feature.devicedetail.DeviceDetailViewModelFactory
import com.chriscartland.blanket.feature.devicetypes.DeviceTypeListViewModel
import com.chriscartland.blanket.feature.devicetypes.EditDeviceTypeViewModelFactory
import com.chriscartland.blanket.feature.editdevice.EditDeviceViewModelFactory
import com.chriscartland.blanket.feature.eventdetail.EventDetailViewModelFactory
import com.chriscartland.blanket.feature.history.HistoryListViewModel
import com.chriscartland.blanket.feature.home.HomeViewModel
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope

@Scope
annotation class Singleton

@Component
@Singleton
abstract class AppComponent(
    private val databaseFactory: DatabaseFactory,
) {
    abstract val homeViewModel: HomeViewModel
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
