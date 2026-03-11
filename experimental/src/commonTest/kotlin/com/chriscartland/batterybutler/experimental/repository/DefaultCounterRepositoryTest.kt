package com.chriscartland.batterybutler.experimental.repository

import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.experimental.datasource.FakeLocalCounterDataSource
import com.chriscartland.batterybutler.experimental.model.CounterError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DefaultCounterRepositoryTest {

    private lateinit var fakeDataSource: FakeLocalCounterDataSource
    private lateinit var repository: DefaultCounterRepository

    @BeforeTest
    fun setUp() {
        fakeDataSource = FakeLocalCounterDataSource()
        repository = DefaultCounterRepository(fakeDataSource)
    }

    @Test
    fun `getCounter returns success with current value`() = runTest {
        fakeDataSource.setCounter(42L)
        val result = repository.getCounter()
        assertIs<Result.Success<Long>>(result)
        assertEquals(42L, result.data)
    }

    @Test
    fun `getCounter returns error when data source throws`() = runTest {
        fakeDataSource.shouldThrowOnRead = true
        val result = repository.getCounter()
        assertIs<Result.Error<CounterError>>(result)
        assertIs<CounterError.ReadFailed>(result.error)
    }

    @Test
    fun `setCounter returns success`() = runTest {
        val result = repository.setCounter(10L)
        assertIs<Result.Success<Unit>>(result)
        assertEquals(10L, fakeDataSource.getCounter())
    }

    @Test
    fun `setCounter returns error when data source throws`() = runTest {
        fakeDataSource.shouldThrowOnWrite = true
        val result = repository.setCounter(10L)
        assertIs<Result.Error<CounterError>>(result)
        assertIs<CounterError.WriteFailed>(result.error)
    }

    @Test
    fun `observeCounter emits values from data source`() = runTest {
        fakeDataSource.setCounter(5L)
        val value = repository.observeCounter().first()
        assertEquals(5L, value)
    }
}
