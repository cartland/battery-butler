package com.chriscartland.batterybutler.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.chriscartland.batterybutler.data.repository.DataStoreNetworkModeRepository
import com.chriscartland.batterybutler.data.repository.DefaultDeviceRepository
import com.chriscartland.batterybutler.datalocal.LocalDataSource
import com.chriscartland.batterybutler.datalocal.RoomLocalDataSource
import com.chriscartland.batterybutler.datalocal.preferences.DataStoreFactory
import com.chriscartland.batterybutler.datalocal.preferences.DataStorePreferencesDataSource
import com.chriscartland.batterybutler.datalocal.preferences.PreferencesDataSource
import com.chriscartland.batterybutler.datalocal.room.AppDatabase
import com.chriscartland.batterybutler.datalocal.room.DatabaseFactory
import com.chriscartland.batterybutler.datalocal.room.DeviceDao
import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Provides

interface BaseDataComponent {
    // Requirements from the platform/app
    val databaseFactory: DatabaseFactory
    val dataStoreFactory: DataStoreFactory
    val appScope: CoroutineScope

    @Provides
    fun provideAppDatabase(): AppDatabase = databaseFactory.createDatabase()

    @Provides
    fun provideDeviceDao(database: AppDatabase): DeviceDao = database.deviceDao()

    @Provides
    fun providePreferencesDataStore(): DataStore<Preferences> = dataStoreFactory.createPreferencesDataStore()

    @Provides
    fun providePreferencesDataSource(dataSource: DataStorePreferencesDataSource): PreferencesDataSource = dataSource

    @Provides
    fun provideDeviceRepository(repo: DefaultDeviceRepository): DeviceRepository = repo

    @Provides
    fun provideNetworkModeRepository(repo: DataStoreNetworkModeRepository): NetworkModeRepository = repo

    @Provides
    fun provideNetworkModeFlow(repo: NetworkModeRepository): Flow<NetworkMode> = repo.networkMode

    @Provides
    fun provideLocalDataSource(dataSource: RoomLocalDataSource): LocalDataSource = dataSource
}
