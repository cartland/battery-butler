package com.chriscartland.batterybutler.shared.di

import com.chriscartland.batterybutler.data.di.DatabaseFactory
import com.chriscartland.batterybutler.data.repository.RoomDeviceRepository
import com.chriscartland.batterybutler.data.room.AppDatabase
import com.chriscartland.batterybutler.data.room.DeviceDao
import com.chriscartland.batterybutler.domain.ai.AiEngine
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.feature.home.HomeViewModel
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
) {
    abstract val homeViewModel: HomeViewModel

    // Can add other ViewModels as needed for the native app

    @Provides
    @SharedSingleton
    fun provideAppDatabase(): AppDatabase = databaseFactory.createDatabase()

    @Provides
    fun provideDeviceDao(database: AppDatabase): DeviceDao = database.deviceDao()

    @Provides
    @SharedSingleton
    fun provideDeviceRepository(repo: RoomDeviceRepository): DeviceRepository = repo
}
