package com.chriscartland.batterybutler.data.di

import com.chriscartland.batterybutler.data.repository.RoomDeviceRepository
import com.chriscartland.batterybutler.data.room.AppDatabase
import com.chriscartland.batterybutler.data.room.DeviceDao
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import me.tatarka.inject.annotations.Provides

interface DataComponent {
    // Requirements from the platform/app
    val databaseFactory: DatabaseFactory

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
    fun provideDeviceDao(database: AppDatabase): DeviceDao = database.deviceDao()

    @Provides
    fun provideDeviceRepository(repo: RoomDeviceRepository): DeviceRepository = repo
}
