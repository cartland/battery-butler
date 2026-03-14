package com.chriscartland.batterybutler.experimental.datalocal.di

import com.chriscartland.batterybutler.experimental.datalocal.DefaultAppCounterService
import com.chriscartland.batterybutler.experimental.datalocal.DefaultCounterRepository
import com.chriscartland.batterybutler.experimental.datalocal.DataStoreCounterDataSource
import com.chriscartland.batterybutler.experimental.datalocal.LocalCounterDataSource
import com.chriscartland.batterybutler.experimental.domain.repository.CounterRepository
import com.chriscartland.batterybutler.experimental.domain.service.AppCounterService
import me.tatarka.inject.annotations.Provides

interface ExperimentalDataComponent {
    @Provides
    fun provideCounterRepository(impl: DefaultCounterRepository): CounterRepository = impl

    @Provides
    fun provideLocalCounterDataSource(impl: DataStoreCounterDataSource): LocalCounterDataSource = impl

    @Provides
    fun provideAppCounterService(impl: DefaultAppCounterService): AppCounterService = impl
}
