package com.chriscartland.batterybutler.experimental.usecase

import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.experimental.datalocal.DefaultCounterRepository
import com.chriscartland.batterybutler.experimental.datalocal.FakeLocalCounterDataSource
import com.chriscartland.batterybutler.experimental.domain.model.CounterError
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StartCounterUseCaseTest {
    private lateinit var fakeDataSource: FakeLocalCounterDataSource
    private lateinit var useCase: StartCounterUseCase

    @BeforeTest
    fun setUp() {
        fakeDataSource = FakeLocalCounterDataSource()
        useCase = StartCounterUseCase(DefaultCounterRepository(fakeDataSource))
    }

    @Test
    fun `invoke increments counter through data source`() =
        runTest {
            val job = launch { useCase(delayMs = 100L) }
            advanceTimeBy(350L)
            job.cancel()
            // After 350ms with 100ms delay: increment at 0ms, 100ms, 200ms, 300ms = 4 increments
            // But first increment happens immediately, then delay, so:
            // t=0: increment (1), delay 100
            // t=100: increment (2), delay 100
            // t=200: increment (3), delay 100
            // t=300: increment (4), delay 100
            assertEquals(4L, fakeDataSource.currentValue)
        }

    @Test
    fun `invoke returns error when increment fails`() =
        runTest {
            fakeDataSource.setWriteError(true)
            val result = useCase(delayMs = 1L)
            assertIs<Result.Error<CounterError>>(result)
            assertIs<CounterError.WriteFailed>(result.error)
        }

    @Test
    fun `invoke writes each increment to data source`() =
        runTest {
            val job = launch { useCase(delayMs = 100L) }
            advanceTimeBy(1L)
            assertEquals(1L, fakeDataSource.currentValue)
            advanceTimeBy(100L)
            assertEquals(2L, fakeDataSource.currentValue)
            advanceTimeBy(100L)
            assertEquals(3L, fakeDataSource.currentValue)
            job.cancel()
        }

    @Test
    fun `invoke stops and returns error when write fails mid-loop`() =
        runTest {
            val resultHolder = mutableListOf<Result<Nothing, CounterError>>()
            val job = launch {
                resultHolder.add(useCase(delayMs = 100L))
            }
            advanceTimeBy(250L)
            fakeDataSource.setWriteError(true)
            advanceTimeBy(100L)
            job.join()
            assertEquals(1, resultHolder.size)
            assertIs<Result.Error<CounterError>>(resultHolder.first())
        }
}
