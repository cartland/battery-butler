package com.chriscartland.batterybutler.experimental.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.experimental.model.CounterState
import com.chriscartland.batterybutler.experimental.usecase.GetCounterUseCase
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
) : ViewModel() {

    private val _state = MutableStateFlow<CounterState>(CounterState.Idle)
    val state: StateFlow<CounterState> = _state

    private var counterJob: Job? = null

    fun start() {
        counterJob?.cancel()
        _state.value = CounterState.Loading
        counterJob = viewModelScope.launch {
            when (val result = startCounterUseCase()) {
                is Result.Success -> {
                    result.data.collect { value ->
                        _state.value = CounterState.Active(value)
                    }
                }
                is Result.Error -> {
                    _state.value = CounterState.Error(result.error.message)
                }
            }
        }
    }

    fun get() {
        viewModelScope.launch {
            _state.value = CounterState.Loading
            when (val result = getCounterUseCase()) {
                is Result.Success -> {
                    _state.value = CounterState.Active(result.data)
                }
                is Result.Error -> {
                    _state.value = CounterState.Error(result.error.message)
                }
            }
        }
    }
}
