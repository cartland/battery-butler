package com.chriscartland.batterybutler.experimental.data

import com.chriscartland.batterybutler.domain.model.DispatcherProvider
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.experimental.domain.repository.CounterRepository
import com.chriscartland.batterybutler.experimental.domain.service.AppScopedCounter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@Inject
class DefaultAppScopedCounter(
    private val appScope: CoroutineScope,
    private val counterRepository: CounterRepository,
    private val dispatcherProvider: DispatcherProvider,
) : AppScopedCounter {
    private var job: Job? = null
    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    override fun start() {
        if (job?.isActive == true) return
        _isRunning.value = true
        job = appScope.launch(dispatcherProvider.default) {
            while (true) {
                when (counterRepository.increment()) {
                    is Result.Success -> delay(DELAY_MS)
                    is Result.Error -> {
                        _isRunning.value = false
                        break
                    }
                }
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
        _isRunning.value = false
    }

    companion object {
        const val DELAY_MS = 1000L
    }
}
