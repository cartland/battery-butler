package com.chriscartland.batterybutler.experimental.usecase

import com.chriscartland.batterybutler.experimental.domain.repository.CounterRepository
import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject

@Inject
class ObserveCounterUseCase(
    private val counterRepository: CounterRepository,
) {
    operator fun invoke(): Flow<Long> = counterRepository.observeCounter()
}
