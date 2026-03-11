package com.chriscartland.batterybutler.experimental.model

sealed class CounterState {
    data object Idle : CounterState()
    data object Loading : CounterState()
    data class Active(val value: Long) : CounterState()
    data class Error(val message: String) : CounterState()
}
