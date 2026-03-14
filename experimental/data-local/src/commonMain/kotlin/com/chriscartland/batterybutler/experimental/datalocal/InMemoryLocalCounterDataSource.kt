package com.chriscartland.batterybutler.experimental.datalocal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import me.tatarka.inject.annotations.Inject

@Inject
class InMemoryLocalCounterDataSource : LocalCounterDataSource {
    private val counterFlow = MutableStateFlow(0L)

    override fun observeCounter(): Flow<Long> = counterFlow.asStateFlow()

    override suspend fun getCounter(): Long = counterFlow.value

    override suspend fun incrementCounter(): Long {
        counterFlow.update { it + 1 }
        return counterFlow.value
    }
}
