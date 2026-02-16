package com.chriscartland.batterybutler.data.di

import com.chriscartland.batterybutler.data.repository.auth.DefaultAuthRepository
import com.chriscartland.batterybutler.datalocal.auth.AuthTokenStorage
import com.chriscartland.batterybutler.datalocal.auth.DataStoreAuthTokenStorage
import com.chriscartland.batterybutler.datanetwork.DelegatingRemoteDataSource
import com.chriscartland.batterybutler.datanetwork.RemoteDataSource
import com.chriscartland.batterybutler.datanetwork.auth.GoogleSignInBridge
import com.chriscartland.batterybutler.datanetwork.grpc.NetworkComponent
import com.chriscartland.batterybutler.domain.repository.AuthRepository
import com.chriscartland.batterybutler.proto.AuthServiceClient
import com.chriscartland.batterybutler.proto.GrpcAuthServiceClient
import com.chriscartland.batterybutler.proto.GrpcSyncServiceClient
import com.chriscartland.batterybutler.proto.SyncServiceClient
import com.squareup.wire.GrpcClient
import me.tatarka.inject.annotations.Provides

interface NetworkingDataComponent {
    // Requirements
    val networkComponent: NetworkComponent
    val googleSignInBridge: GoogleSignInBridge

    @Provides
    fun provideRemoteDataSource(dataSource: DelegatingRemoteDataSource): RemoteDataSource = dataSource

    @Provides
    fun provideSyncServiceClient(client: GrpcClient): SyncServiceClient = GrpcSyncServiceClient(client)

    @Provides
    fun provideAuthServiceClient(client: GrpcClient): AuthServiceClient = GrpcAuthServiceClient(client)

    @Provides
    fun provideAuthTokenStorage(storage: DataStoreAuthTokenStorage): AuthTokenStorage = storage

    @Provides
    fun provideAuthRepository(repo: DefaultAuthRepository): AuthRepository = repo
}
