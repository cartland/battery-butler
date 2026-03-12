package com.chriscartland.batterybutler.experimental.composeapp.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.chriscartland.batterybutler.datalocal.preferences.DataStoreFactory
import com.chriscartland.batterybutler.experimental.datalocal.DataStoreCounterDataSource
import com.chriscartland.batterybutler.experimental.datalocal.DefaultCounterRepository
import com.chriscartland.batterybutler.experimental.datalocal.LocalCounterDataSource
import com.chriscartland.batterybutler.experimental.domain.repository.CounterRepository
import com.chriscartland.batterybutler.experimental.viewmodel.CounterViewModel
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

@Component
@ExperimentalSingleton
abstract class ExperimentalAppComponent(
    private val dataStoreFactory: DataStoreFactory,
) {
    abstract val counterViewModel: CounterViewModel

    @Provides
    @ExperimentalSingleton
    fun providePreferencesDataStore(): DataStore<Preferences> = dataStoreFactory.createPreferencesDataStore()

    @Provides
    @ExperimentalSingleton
    fun provideCounterRepository(impl: DefaultCounterRepository): CounterRepository = impl

    @Provides
    @ExperimentalSingleton
    fun provideLocalCounterDataSource(impl: DataStoreCounterDataSource): LocalCounterDataSource = impl

    companion object
}
