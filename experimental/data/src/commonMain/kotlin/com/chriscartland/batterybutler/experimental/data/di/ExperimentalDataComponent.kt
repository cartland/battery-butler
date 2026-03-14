package com.chriscartland.batterybutler.experimental.data.di

import com.chriscartland.batterybutler.experimental.data.DefaultAppScopedCounter
import com.chriscartland.batterybutler.experimental.data.DefaultCounterRepository
import com.chriscartland.batterybutler.experimental.domain.repository.CounterRepository
import com.chriscartland.batterybutler.experimental.domain.service.AppScopedCounter

interface ExperimentalDataComponent {
    fun provideCounterRepository(impl: DefaultCounterRepository): CounterRepository = impl

    fun provideAppScopedCounter(impl: DefaultAppScopedCounter): AppScopedCounter = impl
}
