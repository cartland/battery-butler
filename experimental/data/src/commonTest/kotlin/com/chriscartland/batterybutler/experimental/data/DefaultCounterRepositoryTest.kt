package com.chriscartland.batterybutler.experimental.data

import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.experimental.datalocal.FakeLocalCounterDataSource
import com.chriscartland.batterybutler.experimental.domain.model.CounterError
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
    fun `get returns success with current value`() =
        runTest {
            fakeDataSource.incrementCounter()
            fakeDataSource.incrementCounter()
            val result = repository.get()
            assertIs<Result.Success<Long>>(result)
            assertEquals(2L, result.data)
        }

    @Test
    fun `get returns error when data source throws`() =
        runTest {
            fakeDataSource.setReadError(true)
            val result = repository.get()
            assertIs<Result.Error<CounterError>>(result)
            assertIs<CounterError.ReadFailed>(result.error)
        }

    @Test
    fun `increment returns success with new value`() =
        runTest {
            val result = repository.increment()
            assertIs<Result.Success<Long>>(result)
            assertEquals(1L, result.data)
        }

    @Test
    fun `increment returns error when data source throws`() =
        runTest {
            fakeDataSource.setWriteError(true)
            val result = repository.increment()
            assertIs<Result.Error<CounterError>>(result)
            assertIs<CounterError.WriteFailed>(result.error)
        }

    @Test
    fun `observeCounter emits values from data source`() =
        runTest {
            fakeDataSource.incrementCounter()
            val value = repository.observeCounter().first()
            assertEquals(1L, value)
        }
}
