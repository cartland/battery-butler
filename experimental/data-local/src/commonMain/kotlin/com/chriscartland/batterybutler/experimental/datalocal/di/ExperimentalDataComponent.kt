package com.chriscartland.batterybutler.experimental.datalocal.di

import com.chriscartland.batterybutler.experimental.datalocal.DefaultAppCounterService
import com.chriscartland.batterybutler.experimental.datalocal.DefaultCounterRepository
import com.chriscartland.batterybutler.experimental.domain.repository.CounterRepository
import com.chriscartland.batterybutler.experimental.domain.service.AppCounterService

interface ExperimentalDataComponent {
    fun provideCounterRepository(impl: DefaultCounterRepository): CounterRepository = impl

    fun provideAppCounterService(impl: DefaultAppCounterService): AppCounterService = impl
}
