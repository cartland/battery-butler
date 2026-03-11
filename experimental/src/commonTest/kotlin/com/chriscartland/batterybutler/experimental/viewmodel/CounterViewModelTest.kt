package com.chriscartland.batterybutler.experimental.viewmodel

import com.chriscartland.batterybutler.experimental.datasource.FakeLocalCounterDataSource
import com.chriscartland.batterybutler.experimental.model.CounterState
import com.chriscartland.batterybutler.experimental.repository.DefaultCounterRepository
import com.chriscartland.batterybutler.experimental.usecase.GetCounterUseCase
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
            counterRepository = repository,
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() = runTest {
        assertIs<CounterState.Idle>(viewModel.state.value)
    }

    @Test
    fun `get updates state to Active with current counter value`() = runTest {
        fakeDataSource.incrementCounter()
        fakeDataSource.incrementCounter()
        viewModel.get()
        testDispatcher.scheduler.advanceUntilIdle()
        assertIs<CounterState.Active>(viewModel.state.value)
        assertEquals(2L, (viewModel.state.value as CounterState.Active).value)
    }

    @Test
    fun `get updates state to Error when read fails`() = runTest {
        fakeDataSource.setReadError(true)
        viewModel.get()
        testDispatcher.scheduler.advanceUntilIdle()
        assertIs<CounterState.Error>(viewModel.state.value)
    }

    @Test
    fun `start transitions through Loading to Active`() = runTest {
        viewModel.start()
        assertIs<CounterState.Loading>(viewModel.state.value)
        testDispatcher.scheduler.advanceUntilIdle()
        assertIs<CounterState.Active>(viewModel.state.value)
        assertEquals(1L, (viewModel.state.value as CounterState.Active).value)
    }

    @Test
    fun `start increments counter over time`() = runTest {
        viewModel.start()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1L, (viewModel.state.value as CounterState.Active).value)

        advanceTimeBy(1001L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(2L, (viewModel.state.value as CounterState.Active).value)

        advanceTimeBy(1000L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(3L, (viewModel.state.value as CounterState.Active).value)
    }

    @Test
    fun `start shows error when write fails`() = runTest {
        fakeDataSource.setWriteError(true)
        viewModel.start()
        testDispatcher.scheduler.advanceUntilIdle()
        assertIs<CounterState.Error>(viewModel.state.value)
    }

    @Test
    fun `stop cancels the counter job`() = runTest {
        viewModel.start()
        testDispatcher.scheduler.advanceUntilIdle()
        assertIs<CounterState.Active>(viewModel.state.value)
        val valueAtStop = (viewModel.state.value as CounterState.Active).value

        viewModel.stop()
        advanceTimeBy(3000L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertIs<CounterState.Active>(viewModel.state.value)
        assertEquals(valueAtStop, (viewModel.state.value as CounterState.Active).value)
    }
}
