package com.chriscartland.batterybutler.experimental.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.DispatcherProvider
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.experimental.domain.model.CounterState
import com.chriscartland.batterybutler.experimental.usecase.GetCounterUseCase
import com.chriscartland.batterybutler.experimental.usecase.ObserveAppScopedCounterRunningUseCase
import com.chriscartland.batterybutler.experimental.usecase.ObserveCounterUseCase
import com.chriscartland.batterybutler.experimental.usecase.RunCounterUseCase
import com.chriscartland.batterybutler.experimental.usecase.StartAppScopedCounterUseCase
import com.chriscartland.batterybutler.experimental.usecase.StopAppScopedCounterUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@Inject
class CounterViewModel(
    private val runCounterUseCase: RunCounterUseCase,
    private val observeCounterUseCase: ObserveCounterUseCase,
    private val getCounterUseCase: GetCounterUseCase,
    private val startAppScopedCounterUseCase: StartAppScopedCounterUseCase,
    private val stopAppScopedCounterUseCase: StopAppScopedCounterUseCase,
    private val observeAppScopedCounterRunningUseCase: ObserveAppScopedCounterRunningUseCase,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val _counterRunning = MutableStateFlow(false)
    val counterRunning: StateFlow<Boolean> = _counterRunning.asStateFlow()

    val appCounterRunning: StateFlow<Boolean> = observeAppScopedCounterRunningUseCase()

    private val _observeState = MutableStateFlow<CounterState>(CounterState.Idle)
    val observeState: StateFlow<CounterState> = _observeState.asStateFlow()

    private val _getState = MutableStateFlow<CounterState>(CounterState.Idle)
    val getState: StateFlow<CounterState> = _getState.asStateFlow()

    private var counterJob: Job? = null
    private var observeJob: Job? = null

    fun startCounter() {
        if (counterJob?.isActive == true) return
        _counterRunning.value = true
        counterJob = viewModelScope.launch(dispatcherProvider.default) {
            runCounterUseCase()
            _counterRunning.value = false
        }
    }

    fun stopCounter() {
        counterJob?.cancel()
        counterJob = null
        _counterRunning.value = false
    }

    fun startAppCounter() {
        startAppScopedCounterUseCase()
    }

    fun stopAppCounter() {
        stopAppScopedCounterUseCase()
    }

    fun startObserving() {
        if (observeJob?.isActive == true) return
        _observeState.value = CounterState.Loading
        observeJob = viewModelScope.launch(dispatcherProvider.main) {
            observeCounterUseCase().collect { value ->
                _observeState.value = CounterState.Active(value)
            }
        }
    }

    fun stopObserving() {
        observeJob?.cancel()
        observeJob = null
        _observeState.value = CounterState.Idle
    }

    fun getOnce() {
        _getState.value = CounterState.Loading
        viewModelScope.launch(dispatcherProvider.main) {
            when (val result = getCounterUseCase()) {
                is Result.Success -> _getState.value = CounterState.Active(result.data)
                is Result.Error -> _getState.value = CounterState.Error(result.error.message)
            }
        }
    }
}
