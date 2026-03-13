package com.chriscartland.batterybutler.experimental.datalocal

import com.chriscartland.batterybutler.domain.model.Result
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class InMemoryCounterRepositoryTest {
    private val repository = InMemoryCounterRepository()

    @Test
    fun get_initialValue_isZero() =
        runTest {
            val result = repository.get()
            assertEquals(Result.Success(0L), result)
        }

    @Test
    fun increment_returnsIncrementedValue() =
        runTest {
            val result = repository.increment()
            assertEquals(Result.Success(1L), result)
        }

    @Test
    fun increment_twice_returnsTwo() =
        runTest {
            repository.increment()
            val result = repository.increment()
            assertEquals(Result.Success(2L), result)
        }

    @Test
    fun get_afterIncrement_returnsCurrentValue() =
        runTest {
            repository.increment()
            repository.increment()
            val result = repository.get()
            assertEquals(Result.Success(2L), result)
        }

    @Test
    fun observeCounter_initialValue_isZero() =
        runTest {
            val value = repository.observeCounter().first()
            assertEquals(0L, value)
        }

    @Test
    fun observeCounter_afterIncrement_emitsUpdatedValue() =
        runTest {
            repository.increment()
            val value = repository.observeCounter().first()
            assertEquals(1L, value)
        }
}
