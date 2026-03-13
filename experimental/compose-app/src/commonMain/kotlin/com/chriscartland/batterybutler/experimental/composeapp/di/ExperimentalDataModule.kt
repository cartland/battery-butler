package com.chriscartland.batterybutler.experimental.composeapp.di

import com.chriscartland.batterybutler.experimental.datalocal.DataStoreCounterDataSource
import com.chriscartland.batterybutler.experimental.datalocal.DefaultCounterRepository
import com.chriscartland.batterybutler.experimental.datalocal.LocalCounterDataSource
import com.chriscartland.batterybutler.experimental.datalocal.di.ExperimentalDataComponent
import com.chriscartland.batterybutler.experimental.domain.repository.CounterRepository
import me.tatarka.inject.annotations.Provides

interface ExperimentalDataModule : ExperimentalDataComponent {
    @Provides
    @ExperimentalSingleton
    override fun provideCounterRepository(impl: DefaultCounterRepository): CounterRepository = super.provideCounterRepository(impl)

    @Provides
    @ExperimentalSingleton
    override fun provideLocalCounterDataSource(impl: DataStoreCounterDataSource): LocalCounterDataSource = super.provideLocalCounterDataSource(impl)
}
