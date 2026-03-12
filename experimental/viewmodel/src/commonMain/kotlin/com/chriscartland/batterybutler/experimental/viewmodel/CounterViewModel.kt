package com.chriscartland.batterybutler.experimental.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.experimental.domain.model.CounterState
import com.chriscartland.batterybutler.experimental.usecase.GetCounterUseCase
import com.chriscartland.batterybutler.experimental.usecase.ObserveCounterUseCase
import com.chriscartland.batterybutler.experimental.usecase.StartCounterUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@Inject
class CounterViewModel(
    private val startCounterUseCase: StartCounterUseCase,
    private val getCounterUseCase: GetCounterUseCase,
    private val observeCounterUseCase: ObserveCounterUseCase,
) : ViewModel() {
    private val _observeState = MutableStateFlow<CounterState>(CounterState.Idle)
    val observeState: StateFlow<CounterState> = _observeState

    private val _getState = MutableStateFlow<CounterState>(CounterState.Idle)
    val getState: StateFlow<CounterState> = _getState

    private val counterJob = MutableStateFlow<Job?>(null)

    fun start() {
        counterJob.value?.cancel()
        _observeState.value = CounterState.Loading
        counterJob.value = viewModelScope.launch {
            val observeJob = launch {
                observeCounterUseCase().collect { value ->
                    _observeState.value = CounterState.Active(value)
                }
            }
            when (val result = startCounterUseCase()) {
                is Result.Success -> Unit // Never reached; loop runs until cancelled or error
                is Result.Error -> {
                    observeJob.cancel()
                    _observeState.value = CounterState.Error(result.error.message)
                }
            }
        }
    }

    fun stop() {
        counterJob.value?.cancel()
        counterJob.value = null
        val currentState = _observeState.value
        if (currentState is CounterState.Active) {
            _observeState.value = CounterState.Active(currentState.value)
        }
    }

    fun get() {
        viewModelScope.launch {
            _getState.value = CounterState.Loading
            when (val result = getCounterUseCase()) {
                is Result.Success -> {
                    _getState.value = CounterState.Active(result.data)
                }
                is Result.Error -> {
                    _getState.value = CounterState.Error(result.error.message)
                }
            }
        }
    }
}
