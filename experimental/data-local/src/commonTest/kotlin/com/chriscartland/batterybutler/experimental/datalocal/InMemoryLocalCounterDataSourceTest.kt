package com.chriscartland.batterybutler.experimental.datalocal

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class InMemoryLocalCounterDataSourceTest {
    private val dataSource = InMemoryLocalCounterDataSource()

    @Test
    fun observeCounter_initialValue_isZero() =
        runTest {
            val value = dataSource.observeCounter().first()
            assertEquals(0L, value)
        }

    @Test
    fun getCounter_initialValue_isZero() =
        runTest {
            val value = dataSource.getCounter()
            assertEquals(0L, value)
        }

    @Test
    fun incrementCounter_returnsIncrementedValue() =
        runTest {
            val result = dataSource.incrementCounter()
            assertEquals(1L, result)
        }

    @Test
    fun incrementCounter_twice_returnsTwo() =
        runTest {
            dataSource.incrementCounter()
            val result = dataSource.incrementCounter()
            assertEquals(2L, result)
        }

    @Test
    fun getCounter_afterIncrement_returnsCurrentValue() =
        runTest {
            dataSource.incrementCounter()
            dataSource.incrementCounter()
            val value = dataSource.getCounter()
            assertEquals(2L, value)
        }

    @Test
    fun observeCounter_afterIncrement_emitsUpdatedValue() =
        runTest {
            dataSource.incrementCounter()
            val value = dataSource.observeCounter().first()
            assertEquals(1L, value)
        }
}
