package com.chriscartland.batterybutler.data.di

import com.chriscartland.batterybutler.data.repository.InMemoryNetworkModeRepository
import com.chriscartland.batterybutler.datalocal.di.DatabaseFactory
import com.chriscartland.batterybutler.datalocal.repository.RoomDeviceRepository
import com.chriscartland.batterybutler.datalocal.room.AppDatabase
import com.chriscartland.batterybutler.datanetwork.DelegatingRemoteDataSource
import com.chriscartland.batterybutler.datanetwork.NetworkComponent
import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import com.chriscartland.batterybutler.domain.repository.RemoteDataSource
import com.chriscartland.batterybutler.proto.GrpcSyncServiceClient
import com.chriscartland.batterybutler.proto.SyncServiceClient
import com.squareup.wire.GrpcClient
import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Provides

interface DataComponent {
    // Requirements from the platform/app
    val databaseFactory: DatabaseFactory
    val networkComponent: NetworkComponent

    // Scope is managed by the Component using this interface (e.g. Singleton in AppComponent)
    // We cannot use @Singleton here because it's an interface, but we can rely on the implementation scope.
    // However, kotlin-inject usually requires scopes on the provides methods if we want them scoped.
    // Given the structure, we'll let the implementation control the scope or rely on the instance creation.
    // Ideally, for Singletons, we define them here but the 'Singleton' annotation needs to be available.
    // Since 'Singleton' is defined in App, we might need a shared scope annotation or just provide open methods.
    // For now, let's keep it simple: providers.

    @Provides
    fun provideAppDatabase(): AppDatabase = databaseFactory.createDatabase()

    @Provides
    fun provideDeviceRepository(repo: RoomDeviceRepository): DeviceRepository = repo

    @Provides
    fun provideNetworkModeRepository(repo: InMemoryNetworkModeRepository): NetworkModeRepository = repo

    @Provides
    fun provideNetworkModeFlow(repo: NetworkModeRepository): Flow<NetworkMode> = repo.networkMode

    @Provides
    fun provideRemoteDataSource(dataSource: DelegatingRemoteDataSource): RemoteDataSource = dataSource

    @Provides
    fun provideSyncServiceClient(client: GrpcClient): SyncServiceClient = GrpcSyncServiceClient(client)
}
