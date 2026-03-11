package com.chriscartland.batterybutler.experimental.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeLocalCounterDataSource : LocalCounterDataSource {

    private val counterFlow = MutableStateFlow(0L)
    var shouldThrowOnRead = false
    var shouldThrowOnWrite = false

    override fun observeCounter(): Flow<Long> = counterFlow

    override suspend fun getCounter(): Long {
        if (shouldThrowOnRead) throw RuntimeException("Fake read error")
        return counterFlow.value
    }

    override suspend fun setCounter(value: Long) {
        if (shouldThrowOnWrite) throw RuntimeException("Fake write error")
        counterFlow.value = value
    }
}
