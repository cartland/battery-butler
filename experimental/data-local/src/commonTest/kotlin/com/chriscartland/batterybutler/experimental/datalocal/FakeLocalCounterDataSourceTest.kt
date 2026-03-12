package com.chriscartland.batterybutler.experimental.datalocal

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FakeLocalCounterDataSourceTest {
    private lateinit var dataSource: FakeLocalCounterDataSource

    @BeforeTest
    fun setUp() {
        dataSource = FakeLocalCounterDataSource()
    }

    @Test
    fun `initial counter value is zero`() =
        runTest {
            assertEquals(0L, dataSource.getCounter())
        }

    @Test
    fun `incrementCounter increments the value`() =
        runTest {
            val first = dataSource.incrementCounter()
            assertEquals(1L, first)
            val second = dataSource.incrementCounter()
            assertEquals(2L, second)
        }

    @Test
    fun `observeCounter emits current value`() =
        runTest {
            dataSource.incrementCounter()
            dataSource.incrementCounter()
            val value = dataSource.observeCounter().first()
            assertEquals(2L, value)
        }

    @Test
    fun `getCounter throws when read error is enabled`() =
        runTest {
            dataSource.setReadError(true)
            assertFailsWith<RuntimeException> {
                dataSource.getCounter()
            }
        }

    @Test
    fun `incrementCounter throws when write error is enabled`() =
        runTest {
            dataSource.setWriteError(true)
            assertFailsWith<RuntimeException> {
                dataSource.incrementCounter()
            }
        }

    @Test
    fun `resetCounter sets value back to zero`() =
        runTest {
            dataSource.incrementCounter()
            dataSource.incrementCounter()
            assertEquals(2L, dataSource.currentValue)
            dataSource.resetCounter()
            assertEquals(0L, dataSource.currentValue)
        }
}
