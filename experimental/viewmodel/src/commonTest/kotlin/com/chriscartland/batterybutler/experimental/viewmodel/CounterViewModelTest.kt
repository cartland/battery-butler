package com.chriscartland.batterybutler.experimental.viewmodel

import com.chriscartland.batterybutler.domain.model.DispatcherProvider
import com.chriscartland.batterybutler.experimental.datalocal.DefaultCounterRepository
import com.chriscartland.batterybutler.experimental.datalocal.FakeAppCounterService
import com.chriscartland.batterybutler.experimental.datalocal.FakeLocalCounterDataSource
import com.chriscartland.batterybutler.experimental.domain.model.CounterState
import com.chriscartland.batterybutler.experimental.usecase.GetCounterUseCase
import com.chriscartland.batterybutler.experimental.usecase.IncrementCounterUseCase
import com.chriscartland.batterybutler.experimental.usecase.ObserveCounterUseCase
import kotlinx.coroutines.CoroutineDispatcher
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CounterViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = object : DispatcherProvider {
        override val default: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val main: CoroutineDispatcher = testDispatcher
    }
    private lateinit var fakeDataSource: FakeLocalCounterDataSource
    private lateinit var fakeAppCounterService: FakeAppCounterService
    private lateinit var viewModel: CounterViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDataSource = FakeLocalCounterDataSource()
        fakeAppCounterService = FakeAppCounterService()
        val repository = DefaultCounterRepository(fakeDataSource)
        viewModel = CounterViewModel(
            incrementCounterUseCase = IncrementCounterUseCase(repository),
            observeCounterUseCase = ObserveCounterUseCase(repository),
            getCounterUseCase = GetCounterUseCase(repository),
            appCounterService = fakeAppCounterService,
            dispatcherProvider = testDispatcherProvider,
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial counterRunning is false`() =
        runTest {
            assertFalse(viewModel.counterRunning.value)
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
    fun `startCounter sets counterRunning to true`() =
        runTest {
            viewModel.startCounter()
            assertTrue(viewModel.counterRunning.value)
            viewModel.stopCounter()
        }

    @Test
    fun `startCounter increments counter over time`() =
        runTest {
            viewModel.startCounter()
            testDispatcher.scheduler.runCurrent()
            assertEquals(1L, fakeDataSource.currentValue)

            advanceTimeBy(1001L)
            testDispatcher.scheduler.runCurrent()
            assertEquals(2L, fakeDataSource.currentValue)

            advanceTimeBy(1000L)
            testDispatcher.scheduler.runCurrent()
            assertEquals(3L, fakeDataSource.currentValue)
            viewModel.stopCounter()
        }

    @Test
    fun `stopCounter sets counterRunning to false`() =
        runTest {
            viewModel.startCounter()
            assertTrue(viewModel.counterRunning.value)
            viewModel.stopCounter()
            assertFalse(viewModel.counterRunning.value)
        }

    @Test
    fun `stopCounter stops incrementing`() =
        runTest {
            viewModel.startCounter()
            testDispatcher.scheduler.runCurrent()
            val valueAtStop = fakeDataSource.currentValue

            viewModel.stopCounter()
            advanceTimeBy(3000L)
            testDispatcher.scheduler.runCurrent()
            assertEquals(valueAtStop, fakeDataSource.currentValue)
        }

    @Test
    fun `startObserving transitions Loading then Active`() =
        runTest {
            fakeDataSource.incrementCounter()
            viewModel.startObserving()
            assertIs<CounterState.Loading>(viewModel.observeState.value)
            testDispatcher.scheduler.runCurrent()
            assertIs<CounterState.Active>(viewModel.observeState.value)
            assertEquals(1L, (viewModel.observeState.value as CounterState.Active).value)
            viewModel.stopObserving()
        }

    @Test
    fun `stopObserving resets to Idle`() =
        runTest {
            fakeDataSource.incrementCounter()
            viewModel.startObserving()
            testDispatcher.scheduler.runCurrent()
            assertIs<CounterState.Active>(viewModel.observeState.value)

            viewModel.stopObserving()
            assertIs<CounterState.Idle>(viewModel.observeState.value)
        }

    @Test
    fun `getOnce does one-shot read`() =
        runTest {
            fakeDataSource.incrementCounter()
            fakeDataSource.incrementCounter()
            viewModel.getOnce()
            assertIs<CounterState.Loading>(viewModel.getState.value)
            testDispatcher.scheduler.advanceUntilIdle()
            assertIs<CounterState.Active>(viewModel.getState.value)
            assertEquals(2L, (viewModel.getState.value as CounterState.Active).value)
        }

    @Test
    fun `getOnce shows error when read fails`() =
        runTest {
            fakeDataSource.setReadError(true)
            viewModel.getOnce()
            testDispatcher.scheduler.advanceUntilIdle()
            assertIs<CounterState.Error>(viewModel.getState.value)
        }

    @Test
    fun `startCounter does not affect observeState`() =
        runTest {
            viewModel.startCounter()
            testDispatcher.scheduler.runCurrent()
            assertIs<CounterState.Idle>(viewModel.observeState.value)
            viewModel.stopCounter()
        }

    @Test
    fun `startObserving does not affect getState`() =
        runTest {
            fakeDataSource.incrementCounter()
            viewModel.startObserving()
            testDispatcher.scheduler.runCurrent()
            assertIs<CounterState.Idle>(viewModel.getState.value)
            viewModel.stopObserving()
        }

    @Test
    fun `counter and observe operate independently`() =
        runTest {
            // Start counter — increments in the background
            viewModel.startCounter()
            testDispatcher.scheduler.runCurrent()
            assertEquals(1L, fakeDataSource.currentValue)

            // Start observing — picks up the current value
            viewModel.startObserving()
            testDispatcher.scheduler.runCurrent()
            assertIs<CounterState.Active>(viewModel.observeState.value)

            // Advance time — counter increments, observer sees updates
            advanceTimeBy(1001L)
            testDispatcher.scheduler.runCurrent()
            assertEquals(2L, fakeDataSource.currentValue)
            assertEquals(2L, (viewModel.observeState.value as CounterState.Active).value)

            // Stop counter — observer still active, value frozen
            viewModel.stopCounter()
            assertFalse(viewModel.counterRunning.value)
            assertIs<CounterState.Active>(viewModel.observeState.value)

            // Stop observing
            viewModel.stopObserving()
            assertIs<CounterState.Idle>(viewModel.observeState.value)
        }

    @Test
    fun `write error stops counter`() =
        runTest {
            fakeDataSource.setWriteError(true)
            viewModel.startCounter()
            testDispatcher.scheduler.advanceUntilIdle()
            assertFalse(viewModel.counterRunning.value)
        }

    @Test
    fun `getOnce snapshot while observe continues`() =
        runTest {
            viewModel.startCounter()
            viewModel.startObserving()
            testDispatcher.scheduler.runCurrent()

            viewModel.getOnce()
            testDispatcher.scheduler.runCurrent()
            assertEquals(1L, (viewModel.getState.value as CounterState.Active).value)

            advanceTimeBy(1001L)
            testDispatcher.scheduler.runCurrent()
            assertEquals(2L, (viewModel.observeState.value as CounterState.Active).value)
            // getState stays at 1 until tapped again
            assertEquals(1L, (viewModel.getState.value as CounterState.Active).value)
            viewModel.stopCounter()
            viewModel.stopObserving()
        }

    @Test
    fun `initial appCounterRunning is false`() =
        runTest {
            assertFalse(viewModel.appCounterRunning.value)
        }

    @Test
    fun `startAppCounter delegates to service`() =
        runTest {
            viewModel.startAppCounter()
            assertEquals(1, fakeAppCounterService.startCallCount)
            assertTrue(viewModel.appCounterRunning.value)
        }

    @Test
    fun `stopAppCounter delegates to service`() =
        runTest {
            viewModel.startAppCounter()
            viewModel.stopAppCounter()
            assertEquals(1, fakeAppCounterService.stopCallCount)
            assertFalse(viewModel.appCounterRunning.value)
        }

    @Test
    fun `app counter and VM counter are independent`() =
        runTest {
            viewModel.startAppCounter()
            assertTrue(viewModel.appCounterRunning.value)
            assertFalse(viewModel.counterRunning.value)

            viewModel.startCounter()
            assertTrue(viewModel.counterRunning.value)
            assertTrue(viewModel.appCounterRunning.value)

            viewModel.stopCounter()
            assertFalse(viewModel.counterRunning.value)
            assertTrue(viewModel.appCounterRunning.value)

            viewModel.stopAppCounter()
            assertFalse(viewModel.appCounterRunning.value)
        }

    @Test
    fun `startAppCounter does not affect observeState or getState`() =
        runTest {
            viewModel.startAppCounter()
            assertIs<CounterState.Idle>(viewModel.observeState.value)
            assertIs<CounterState.Idle>(viewModel.getState.value)
            viewModel.stopAppCounter()
        }
}
