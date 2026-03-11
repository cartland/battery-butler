package com.chriscartland.batterybutler.experimental.usecase

import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.experimental.model.CounterError
import com.chriscartland.batterybutler.experimental.repository.CounterRepository
import me.tatarka.inject.annotations.Inject

@Inject
class GetCounterUseCase(
    private val counterRepository: CounterRepository,
) {
    suspend operator fun invoke(): Result<Long, CounterError> =
        counterRepository.get()
}
