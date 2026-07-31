package com.chriscartland.batterybutler.composeapp.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.chriscartland.batterybutler.data.di.DataComponent
import com.chriscartland.batterybutler.data.repository.DataStoreDataModeRepository
import com.chriscartland.batterybutler.data.repository.DataStoreDisplayDensityRepository
import com.chriscartland.batterybutler.data.repository.DefaultDeviceImageRepository
import com.chriscartland.batterybutler.data.repository.DefaultDeviceRepository
import com.chriscartland.batterybutler.data.repository.DefaultSyncManager
import com.chriscartland.batterybutler.data.repository.SyncManager
import com.chriscartland.batterybutler.data.repository.auth.DefaultAuthRepository
import com.chriscartland.batterybutler.datalocal.auth.AuthTokenStorage
import com.chriscartland.batterybutler.datalocal.auth.DataStoreAuthTokenStorage
import com.chriscartland.batterybutler.datalocal.auth.DataStoreLabsRefreshTokenPersistence
import com.chriscartland.batterybutler.datalocal.auth.DataStoreLabsSessionStorage
import com.chriscartland.batterybutler.datalocal.auth.LabsSessionStorage
import com.chriscartland.batterybutler.datalocal.preferences.DataStorePreferencesDataSource
import com.chriscartland.batterybutler.datalocal.preferences.PreferencesDataSource
import com.chriscartland.batterybutler.datalocal.room.AppDatabase
import com.chriscartland.batterybutler.datanetwork.DelegatingDeviceImageDataSource
import com.chriscartland.batterybutler.datanetwork.DelegatingRemoteDataSource
import com.chriscartland.batterybutler.datanetwork.DeviceImageDataSource
import com.chriscartland.batterybutler.datanetwork.RemoteDataSource
import com.chriscartland.batterybutler.domain.repository.AuthRepository
import com.chriscartland.batterybutler.domain.repository.DataModeRepository
import com.chriscartland.batterybutler.domain.repository.DeviceImageRepository
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.domain.repository.DisplayDensityRepository
import com.chriscartland.batterybutler.domain.repository.LabsRefreshTokenPersistence
import me.tatarka.inject.annotations.Provides

interface AppDataModule : DataComponent {
    @Provides
    @Singleton
    override fun provideAppDatabase(): AppDatabase = super.provideAppDatabase()

    @Provides
    @Singleton
    override fun providePreferencesDataStore(): DataStore<Preferences> = super.providePreferencesDataStore()

    @Provides
    @Singleton
    override fun providePreferencesDataSource(dataSource: DataStorePreferencesDataSource): PreferencesDataSource = super.providePreferencesDataSource(dataSource)

    @Provides
    @Singleton
    override fun provideDataModeRepository(repo: DataStoreDataModeRepository): DataModeRepository = super.provideDataModeRepository(repo)

    @Provides
    @Singleton
    override fun provideDisplayDensityRepository(repo: DataStoreDisplayDensityRepository): DisplayDensityRepository = super.provideDisplayDensityRepository(repo)

    @Provides
    @Singleton
    override fun provideRemoteDataSource(dataSource: DelegatingRemoteDataSource): RemoteDataSource = super.provideRemoteDataSource(dataSource)

    @Provides
    @Singleton
    override fun provideDeviceImageDataSource(dataSource: DelegatingDeviceImageDataSource): DeviceImageDataSource = super.provideDeviceImageDataSource(dataSource)

    @Provides
    @Singleton
    override fun provideSyncManager(manager: DefaultSyncManager): SyncManager = super.provideSyncManager(manager)

    @Provides
    @Singleton
    override fun provideDeviceRepository(repo: DefaultDeviceRepository): DeviceRepository = super.provideDeviceRepository(repo)

    @Provides
    @Singleton
    override fun provideDeviceImageRepository(repo: DefaultDeviceImageRepository): DeviceImageRepository = super.provideDeviceImageRepository(repo)

    @Provides
    @Singleton
    override fun provideAuthTokenStorage(storage: DataStoreAuthTokenStorage): AuthTokenStorage = super.provideAuthTokenStorage(storage)

    @Provides
    @Singleton
    override fun provideLabsSessionStorage(storage: DataStoreLabsSessionStorage): LabsSessionStorage = super.provideLabsSessionStorage(storage)

    @Provides
    @Singleton
    override fun provideLabsRefreshTokenPersistence(storage: DataStoreLabsRefreshTokenPersistence): LabsRefreshTokenPersistence = super.provideLabsRefreshTokenPersistence(storage)

    @Provides
    @Singleton
    override fun provideAuthRepository(repo: DefaultAuthRepository): AuthRepository = super.provideAuthRepository(repo)
}
