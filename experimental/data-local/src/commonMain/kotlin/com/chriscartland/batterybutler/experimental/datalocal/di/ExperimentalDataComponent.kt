package com.chriscartland.batterybutler.experimental.datalocal.di

import com.chriscartland.batterybutler.experimental.datalocal.DefaultCounterRepository
import com.chriscartland.batterybutler.experimental.datalocal.InMemoryLocalCounterDataSource
import com.chriscartland.batterybutler.experimental.datalocal.LocalCounterDataSource
import com.chriscartland.batterybutler.experimental.domain.repository.CounterRepository
import me.tatarka.inject.annotations.Provides

interface ExperimentalDataComponent {
    @Provides
    fun provideCounterRepository(impl: DefaultCounterRepository): CounterRepository = impl

    @Provides
    fun provideLocalCounterDataSource(impl: InMemoryLocalCounterDataSource): LocalCounterDataSource = impl
}
