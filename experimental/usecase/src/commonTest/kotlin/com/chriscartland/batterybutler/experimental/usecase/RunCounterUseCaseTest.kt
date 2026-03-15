package com.chriscartland.batterybutler.experimental.usecase

import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.experimental.data.DefaultCounterRepository
import com.chriscartland.batterybutler.experimental.datalocal.FakeLocalCounterDataSource
import com.chriscartland.batterybutler.experimental.domain.model.CounterError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class RunCounterUseCaseTest {
    private lateinit var fakeDataSource: FakeLocalCounterDataSource
    private lateinit var useCase: RunCounterUseCase

    @BeforeTest
    fun setUp() {
        fakeDataSource = FakeLocalCounterDataSource()
        useCase = RunCounterUseCase(DefaultCounterRepository(fakeDataSource))
    }

    @Test
    fun `loop increments counter at expected intervals`() =
        runTest {
            val job = launch { useCase(delayMs = 1000L) }
            testScheduler.runCurrent()
            assertEquals(1L, fakeDataSource.currentValue)

            advanceTimeBy(1001L)
            testScheduler.runCurrent()
            assertEquals(2L, fakeDataSource.currentValue)

            advanceTimeBy(1000L)
            testScheduler.runCurrent()
            assertEquals(3L, fakeDataSource.currentValue)

            job.cancel()
        }

    @Test
    fun `returns error when write fails mid-loop`() =
        runTest {
            // Let it increment once successfully
            val job = launch { useCase(delayMs = 1000L) }
            testScheduler.runCurrent()
            assertEquals(1L, fakeDataSource.currentValue)

            // Now make writes fail
            fakeDataSource.setWriteError(true)
            advanceTimeBy(1001L)
            testScheduler.runCurrent()

            // Job should have completed with error
            val result = job.also { it.join() }
            assertEquals(1L, fakeDataSource.currentValue)
        }

    @Test
    fun `returns error result on write failure`() =
        runTest {
            fakeDataSource.setWriteError(true)
            val result = useCase(delayMs = 1000L)
            assertIs<Result.Error<CounterError>>(result)
            assertIs<CounterError.WriteFailed>(result.error)
        }

    @Test
    fun `cancellation stops the loop cleanly`() =
        runTest {
            val job = launch { useCase(delayMs = 1000L) }
            testScheduler.runCurrent()
            assertEquals(1L, fakeDataSource.currentValue)

            job.cancel()
            advanceTimeBy(3000L)
            testScheduler.runCurrent()
            // Counter should not have advanced after cancellation
            assertEquals(1L, fakeDataSource.currentValue)
        }
}
