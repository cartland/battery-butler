package com.chriscartland.batterybutler.experimental.composeapp.di

import com.chriscartland.batterybutler.experimental.data.DefaultAppScopedCounter
import com.chriscartland.batterybutler.experimental.data.DefaultCounterRepository
import com.chriscartland.batterybutler.experimental.data.di.ExperimentalDataComponent
import com.chriscartland.batterybutler.experimental.datalocal.InMemoryLocalCounterDataSource
import com.chriscartland.batterybutler.experimental.datalocal.LocalCounterDataSource
import com.chriscartland.batterybutler.experimental.domain.repository.CounterRepository
import com.chriscartland.batterybutler.experimental.domain.service.AppScopedCounter
import me.tatarka.inject.annotations.Provides

interface ExperimentalDataModule : ExperimentalDataComponent {
    @Provides
    @ExperimentalSingleton
    override fun provideCounterRepository(impl: DefaultCounterRepository): CounterRepository = super.provideCounterRepository(impl)

    @Provides
    @ExperimentalSingleton
    fun provideLocalCounterDataSource(impl: InMemoryLocalCounterDataSource): LocalCounterDataSource = impl

    @Provides
    @ExperimentalSingleton
    override fun provideAppScopedCounter(impl: DefaultAppScopedCounter): AppScopedCounter = super.provideAppScopedCounter(impl)
}
