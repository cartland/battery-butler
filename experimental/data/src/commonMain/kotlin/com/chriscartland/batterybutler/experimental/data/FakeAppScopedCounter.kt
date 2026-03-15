package com.chriscartland.batterybutler.experimental.data

import com.chriscartland.batterybutler.experimental.domain.service.AppScopedCounter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Test double for [AppScopedCounter]. Lives in commonMain (not commonTest) because
 * it is consumed by tests in other modules (experimental:viewmodel).
 * This follows the same pattern as [FakeLocalCounterDataSource].
 */
class FakeAppScopedCounter : AppScopedCounter {
    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    var startCallCount = 0
        private set
    var stopCallCount = 0
        private set

    override fun start() {
        startCallCount++
        _isRunning.value = true
    }

    override fun stop() {
        stopCallCount++
        _isRunning.value = false
    }

    fun reset() {
        startCallCount = 0
        stopCallCount = 0
        _isRunning.value = false
    }
}
