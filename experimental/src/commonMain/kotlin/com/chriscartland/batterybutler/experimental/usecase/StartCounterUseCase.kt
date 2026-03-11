package com.chriscartland.batterybutler.experimental.usecase

import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.experimental.model.CounterError
import com.chriscartland.batterybutler.experimental.repository.CounterRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import me.tatarka.inject.annotations.Inject

@Inject
class StartCounterUseCase(
    private val counterRepository: CounterRepository,
) {
    suspend operator fun invoke(delayMs: Long = DEFAULT_DELAY_MS): Result<Flow<Long>, CounterError> {
        val counterFlow = flow {
            var counter = 0L
            val setResult = counterRepository.setCounter(counter)
            if (setResult is Result.Error) return@flow
            emit(counter)
            while (true) {
                delay(delayMs)
                counter++
                when (val result = counterRepository.setCounter(counter)) {
                    is Result.Success -> emit(counter)
                    is Result.Error -> return@flow
                }
            }
        }
        return Result.Success(counterFlow)
    }

    companion object {
        const val DEFAULT_DELAY_MS = 1000L
    }
}
