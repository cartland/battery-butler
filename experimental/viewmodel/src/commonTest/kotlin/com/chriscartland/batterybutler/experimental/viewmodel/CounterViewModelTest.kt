package com.chriscartland.batterybutler.experimental.viewmodel

import com.chriscartland.batterybutler.experimental.datalocal.DefaultCounterRepository
import com.chriscartland.batterybutler.experimental.datalocal.FakeLocalCounterDataSource
import com.chriscartland.batterybutler.experimental.domain.model.CounterState
import com.chriscartland.batterybutler.experimental.usecase.GetCounterUseCase
import com.chriscartland.batterybutler.experimental.usecase.ObserveCounterUseCase
import com.chriscartland.batterybutler.experimental.usecase.StartCounterUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class CounterViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDataSource: FakeLocalCounterDataSource
    private lateinit var viewModel: CounterViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDataSource = FakeLocalCounterDataSource()
        val repository = DefaultCounterRepository(fakeDataSource)
        viewModel = CounterViewModel(
            startCounterUseCase = StartCounterUseCase(repository),
            getCounterUseCase = GetCounterUseCase(repository),
            observeCounterUseCase = ObserveCounterUseCase(repository),
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial observeState is Idle`() =
        runTest {
            assertIs<CounterState.Idle>(viewModel.observeState.value)
        }

    @Test
    fun `initial getState is Idle`() =
        runTest {
            assertIs<CounterState.Idle>(viewModel.getState.value)
        }

    @Test
    fun `get updates getState to Active with current counter value`() =
        runTest {
            fakeDataSource.incrementCounter()
            fakeDataSource.incrementCounter()
            viewModel.get()
            testDispatcher.scheduler.advanceUntilIdle()
            assertIs<CounterState.Active>(viewModel.getState.value)
            assertEquals(2L, (viewModel.getState.value as CounterState.Active).value)
        }

    @Test
    fun `get does not affect observeState`() =
        runTest {
            fakeDataSource.incrementCounter()
            viewModel.get()
            testDispatcher.scheduler.advanceUntilIdle()
            assertIs<CounterState.Idle>(viewModel.observeState.value)
        }

    @Test
    fun `get updates state to Error when read fails`() =
        runTest {
            fakeDataSource.setReadError(true)
            viewModel.get()
            testDispatcher.scheduler.advanceUntilIdle()
            assertIs<CounterState.Error>(viewModel.getState.value)
        }

    @Test
    fun `start transitions through Loading to Active`() =
        runTest {
            viewModel.start()
            assertIs<CounterState.Loading>(viewModel.observeState.value)
            // Use targeted advancement instead of advanceUntilIdle() because
            // startCounterUseCase runs an infinite loop that never becomes idle.
            testDispatcher.scheduler.runCurrent()
            assertIs<CounterState.Active>(viewModel.observeState.value)
            assertEquals(1L, (viewModel.observeState.value as CounterState.Active).value)
            viewModel.stop()
        }

    @Test
    fun `start does not affect getState`() =
        runTest {
            viewModel.start()
            testDispatcher.scheduler.runCurrent()
            assertIs<CounterState.Idle>(viewModel.getState.value)
            viewModel.stop()
        }

    @Test
    fun `start increments counter over time`() =
        runTest {
            viewModel.start()
            testDispatcher.scheduler.runCurrent()
            assertEquals(1L, (viewModel.observeState.value as CounterState.Active).value)

            advanceTimeBy(1001L)
            testDispatcher.scheduler.runCurrent()
            assertEquals(2L, (viewModel.observeState.value as CounterState.Active).value)

            advanceTimeBy(1000L)
            testDispatcher.scheduler.runCurrent()
            assertEquals(3L, (viewModel.observeState.value as CounterState.Active).value)
            viewModel.stop()
        }

    @Test
    fun `start shows error when write fails`() =
        runTest {
            fakeDataSource.setWriteError(true)
            viewModel.start()
            testDispatcher.scheduler.advanceUntilIdle()
            assertIs<CounterState.Error>(viewModel.observeState.value)
        }

    @Test
    fun `stop cancels the counter job`() =
        runTest {
            viewModel.start()
            testDispatcher.scheduler.runCurrent()
            assertIs<CounterState.Active>(viewModel.observeState.value)
            val valueAtStop = (viewModel.observeState.value as CounterState.Active).value

            viewModel.stop()
            advanceTimeBy(3000L)
            testDispatcher.scheduler.runCurrent()

            assertIs<CounterState.Active>(viewModel.observeState.value)
            assertEquals(valueAtStop, (viewModel.observeState.value as CounterState.Active).value)
        }

    @Test
    fun `get shows snapshot while observe continues updating`() =
        runTest {
            viewModel.start()
            testDispatcher.scheduler.runCurrent()
            assertEquals(1L, (viewModel.observeState.value as CounterState.Active).value)

            viewModel.get()
            testDispatcher.scheduler.runCurrent()
            assertEquals(1L, (viewModel.getState.value as CounterState.Active).value)

            advanceTimeBy(1001L)
            testDispatcher.scheduler.runCurrent()
            assertEquals(2L, (viewModel.observeState.value as CounterState.Active).value)
            // getState stays at 1 until tapped again
            assertEquals(1L, (viewModel.getState.value as CounterState.Active).value)
            viewModel.stop()
        }
}
