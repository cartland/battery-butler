package com.chriscartland.batterybutler.experimental.composeapp.di

import com.chriscartland.batterybutler.experimental.datalocal.DefaultAppCounterService
import com.chriscartland.batterybutler.experimental.datalocal.DefaultCounterRepository
import com.chriscartland.batterybutler.experimental.datalocal.InMemoryLocalCounterDataSource
import com.chriscartland.batterybutler.experimental.datalocal.LocalCounterDataSource
import com.chriscartland.batterybutler.experimental.datalocal.di.ExperimentalDataComponent
import com.chriscartland.batterybutler.experimental.domain.repository.CounterRepository
import com.chriscartland.batterybutler.experimental.domain.service.AppCounterService
import me.tatarka.inject.annotations.Provides

interface ExperimentalDataModule : ExperimentalDataComponent {
    @Provides
    @ExperimentalSingleton
    override fun provideCounterRepository(impl: DefaultCounterRepository): CounterRepository = super.provideCounterRepository(impl)

    @Provides
    @ExperimentalSingleton
    override fun provideLocalCounterDataSource(impl: InMemoryLocalCounterDataSource): LocalCounterDataSource = super.provideLocalCounterDataSource(impl)

    @Provides
    @ExperimentalSingleton
    override fun provideAppCounterService(impl: DefaultAppCounterService): AppCounterService = super.provideAppCounterService(impl)
}
