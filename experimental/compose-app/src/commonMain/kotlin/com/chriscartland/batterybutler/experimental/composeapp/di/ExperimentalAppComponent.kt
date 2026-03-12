package com.chriscartland.batterybutler.experimental.composeapp.di

import com.chriscartland.batterybutler.experimental.datalocal.DefaultCounterRepository
import com.chriscartland.batterybutler.experimental.datalocal.FakeLocalCounterDataSource
import com.chriscartland.batterybutler.experimental.datalocal.LocalCounterDataSource
import com.chriscartland.batterybutler.experimental.domain.repository.CounterRepository
import com.chriscartland.batterybutler.experimental.viewmodel.CounterViewModel
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

@Component
@ExperimentalSingleton
abstract class ExperimentalAppComponent {
    abstract val counterViewModel: CounterViewModel

    @Provides
    @ExperimentalSingleton
    fun provideCounterRepository(impl: DefaultCounterRepository): CounterRepository = impl

    @Provides
    @ExperimentalSingleton
    fun provideLocalCounterDataSource(): LocalCounterDataSource = FakeLocalCounterDataSource()

    companion object
}
