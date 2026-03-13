package com.chriscartland.batterybutler.experimental.datalocal

import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.experimental.domain.model.CounterError
import com.chriscartland.batterybutler.experimental.domain.repository.CounterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import me.tatarka.inject.annotations.Inject

@Inject
class InMemoryCounterRepository : CounterRepository {
    private val counter = MutableStateFlow(0L)

    override fun observeCounter(): Flow<Long> = counter.asStateFlow()

    override suspend fun get(): Result<Long, CounterError> = Result.Success(counter.value)

    override suspend fun increment(): Result<Long, CounterError> {
        counter.update { it + 1 }
        return Result.Success(counter.value)
    }
}
