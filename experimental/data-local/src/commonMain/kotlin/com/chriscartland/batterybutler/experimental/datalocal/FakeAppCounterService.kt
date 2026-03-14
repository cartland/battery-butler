package com.chriscartland.batterybutler.experimental.datalocal

import com.chriscartland.batterybutler.experimental.domain.service.AppCounterService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Test double for [AppCounterService]. Lives in commonMain (not commonTest) because
 * it is consumed by tests in other modules (experimental:viewmodel).
 * This follows the same pattern as [FakeLocalCounterDataSource].
 */
class FakeAppCounterService : AppCounterService {
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
