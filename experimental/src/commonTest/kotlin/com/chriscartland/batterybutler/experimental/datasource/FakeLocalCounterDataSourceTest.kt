package com.chriscartland.batterybutler.experimental.datasource

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
    fun `initial counter value is zero`() = runTest {
        assertEquals(0L, dataSource.getCounter())
    }

    @Test
    fun `setCounter updates the value`() = runTest {
        dataSource.setCounter(100L)
        assertEquals(100L, dataSource.getCounter())
    }

    @Test
    fun `observeCounter emits current value`() = runTest {
        dataSource.setCounter(55L)
        val value = dataSource.observeCounter().first()
        assertEquals(55L, value)
    }

    @Test
    fun `getCounter throws when shouldThrowOnRead is true`() = runTest {
        dataSource.shouldThrowOnRead = true
        assertFailsWith<RuntimeException> {
            dataSource.getCounter()
        }
    }

    @Test
    fun `setCounter throws when shouldThrowOnWrite is true`() = runTest {
        dataSource.shouldThrowOnWrite = true
        assertFailsWith<RuntimeException> {
            dataSource.setCounter(1L)
        }
    }
}
