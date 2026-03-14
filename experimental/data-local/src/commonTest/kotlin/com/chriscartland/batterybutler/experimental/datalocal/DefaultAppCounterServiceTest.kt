package com.chriscartland.batterybutler.experimental.datalocal

import com.chriscartland.batterybutler.domain.model.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAppCounterServiceTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = object : DispatcherProvider {
        override val default: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val main: CoroutineDispatcher = testDispatcher
    }
    private lateinit var fakeDataSource: FakeLocalCounterDataSource
    private lateinit var repository: DefaultCounterRepository
    private lateinit var service: DefaultAppCounterService

    @BeforeTest
    fun setUp() {
        fakeDataSource = FakeLocalCounterDataSource()
        repository = DefaultCounterRepository(fakeDataSource)
        service = DefaultAppCounterService(
            appScope = CoroutineScope(SupervisorJob() + testDispatcher),
            counterRepository = repository,
            dispatcherProvider = testDispatcherProvider,
        )
    }

    @Test
    fun `initial isRunning is false`() =
        runTest {
            assertFalse(service.isRunning.value)
        }

    @Test
    fun `start sets isRunning to true`() =
        runTest(testDispatcher) {
            service.start()
            assertTrue(service.isRunning.value)
            service.stop()
        }

    @Test
    fun `start increments counter`() =
        runTest(testDispatcher) {
            service.start()
            testDispatcher.scheduler.runCurrent()
            assertEquals(1L, fakeDataSource.currentValue)
            service.stop()
        }

    @Test
    fun `increments over time`() =
        runTest(testDispatcher) {
            service.start()
            testDispatcher.scheduler.runCurrent()
            assertEquals(1L, fakeDataSource.currentValue)

            advanceTimeBy(1001L)
            testDispatcher.scheduler.runCurrent()
            assertEquals(2L, fakeDataSource.currentValue)

            advanceTimeBy(1000L)
            testDispatcher.scheduler.runCurrent()
            assertEquals(3L, fakeDataSource.currentValue)
            service.stop()
        }

    @Test
    fun `stop cancels incrementing`() =
        runTest(testDispatcher) {
            service.start()
            testDispatcher.scheduler.runCurrent()
            val valueAtStop = fakeDataSource.currentValue

            service.stop()
            advanceTimeBy(3000L)
            testDispatcher.scheduler.runCurrent()
            assertEquals(valueAtStop, fakeDataSource.currentValue)
        }

    @Test
    fun `stop sets isRunning to false`() =
        runTest(testDispatcher) {
            service.start()
            assertTrue(service.isRunning.value)
            service.stop()
            assertFalse(service.isRunning.value)
        }

    @Test
    fun `start is idempotent`() =
        runTest(testDispatcher) {
            service.start()
            testDispatcher.scheduler.runCurrent()
            assertEquals(1L, fakeDataSource.currentValue)

            // Second start should be a no-op
            service.start()
            advanceTimeBy(1001L)
            testDispatcher.scheduler.runCurrent()
            // Should be 2, not 3 (only one loop running)
            assertEquals(2L, fakeDataSource.currentValue)
            service.stop()
        }

    @Test
    fun `write error stops service`() =
        runTest(testDispatcher) {
            fakeDataSource.setWriteError(true)
            service.start()
            testDispatcher.scheduler.advanceUntilIdle()
            assertFalse(service.isRunning.value)
        }

    @Test
    fun `can restart after stop`() =
        runTest(testDispatcher) {
            service.start()
            testDispatcher.scheduler.runCurrent()
            assertEquals(1L, fakeDataSource.currentValue)

            service.stop()
            assertFalse(service.isRunning.value)

            service.start()
            testDispatcher.scheduler.runCurrent()
            assertEquals(2L, fakeDataSource.currentValue)
            assertTrue(service.isRunning.value)
            service.stop()
        }
}
