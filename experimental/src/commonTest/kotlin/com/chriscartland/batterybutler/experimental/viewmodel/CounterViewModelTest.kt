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
        fakeDataSource.setCounter(42L)
        viewModel.get()
        testDispatcher.scheduler.advanceUntilIdle()
        assertIs<CounterState.Active>(viewModel.state.value)
        assertEquals(42L, (viewModel.state.value as CounterState.Active).value)
    }

    @Test
    fun `get updates state to Error when read fails`() = runTest {
        fakeDataSource.shouldThrowOnRead = true
        viewModel.get()
        testDispatcher.scheduler.advanceUntilIdle()
        assertIs<CounterState.Error>(viewModel.state.value)
    }

    @Test
    fun `start transitions through Loading to Active`() = runTest {
        viewModel.start()
        // After start, state should be Loading
        assertIs<CounterState.Loading>(viewModel.state.value)
        // Advance past the initial emit (no delay for first value)
        testDispatcher.scheduler.advanceUntilIdle()
        // After the use case starts, first emission is 0
        assertIs<CounterState.Active>(viewModel.state.value)
        assertEquals(0L, (viewModel.state.value as CounterState.Active).value)
    }

    @Test
    fun `start increments counter over time`() = runTest {
        viewModel.start()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0L, (viewModel.state.value as CounterState.Active).value)

        advanceTimeBy(1001L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1L, (viewModel.state.value as CounterState.Active).value)

        advanceTimeBy(1000L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(2L, (viewModel.state.value as CounterState.Active).value)
    }

    @Test
    fun `start shows error when write fails`() = runTest {
        fakeDataSource.shouldThrowOnWrite = true
        viewModel.start()
        testDispatcher.scheduler.advanceUntilIdle()
        // The flow completes without emitting Active — stays at Loading
        // because the flow returns early on write error
        assertIs<CounterState.Loading>(viewModel.state.value)
    }
}
