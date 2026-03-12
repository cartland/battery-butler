package com.chriscartland.batterybutler.experimental.datalocal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeLocalCounterDataSource(
    private val shouldThrowOnRead: MutableStateFlow<Boolean> = MutableStateFlow(false),
    private val shouldThrowOnWrite: MutableStateFlow<Boolean> = MutableStateFlow(false),
) : LocalCounterDataSource {
    private val counterFlow = MutableStateFlow(0L)

    val currentValue: Long get() = counterFlow.value

    override fun observeCounter(): Flow<Long> = counterFlow

    override suspend fun getCounter(): Long {
        if (shouldThrowOnRead.value) throw RuntimeException("Fake read error")
        return counterFlow.value
    }

    override suspend fun incrementCounter(): Long {
        if (shouldThrowOnWrite.value) throw RuntimeException("Fake write error")
        counterFlow.update { it + 1 }
        return counterFlow.value
    }

    fun setReadError(enabled: Boolean) {
        shouldThrowOnRead.value = enabled
    }

    fun setWriteError(enabled: Boolean) {
        shouldThrowOnWrite.value = enabled
    }

    fun resetCounter() {
        counterFlow.value = 0L
    }
}
