package com.chriscartland.batterybutler.experimental.usecase

import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.experimental.domain.model.CounterError
import com.chriscartland.batterybutler.experimental.domain.repository.CounterRepository
import kotlinx.coroutines.delay
import me.tatarka.inject.annotations.Inject

@Inject
class RunCounterUseCase(
    private val counterRepository: CounterRepository,
) {
    suspend operator fun invoke(delayMs: Long = DEFAULT_DELAY_MS): Result<Nothing, CounterError> {
        while (true) {
            when (val result = counterRepository.increment()) {
                is Result.Success -> delay(delayMs)
                is Result.Error -> return result
            }
        }
    }

    companion object {
        const val DEFAULT_DELAY_MS = 1000L
    }
}
