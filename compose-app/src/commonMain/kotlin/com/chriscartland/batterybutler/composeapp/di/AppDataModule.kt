package com.chriscartland.batterybutler.composeapp.di

import com.chriscartland.batterybutler.data.di.DataComponent
import com.chriscartland.batterybutler.data.repository.DefaultDeviceRepository
import com.chriscartland.batterybutler.data.repository.InMemoryNetworkModeRepository
import com.chriscartland.batterybutler.datalocal.room.AppDatabase
import com.chriscartland.batterybutler.datanetwork.DelegatingRemoteDataSource
import com.chriscartland.batterybutler.datanetwork.RemoteDataSource
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import me.tatarka.inject.annotations.Provides

interface AppDataModule : DataComponent {
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
}
