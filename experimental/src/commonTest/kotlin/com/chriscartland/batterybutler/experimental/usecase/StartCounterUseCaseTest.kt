package com.chriscartland.batterybutler.experimental.usecase

import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.experimental.datasource.FakeLocalCounterDataSource
import com.chriscartland.batterybutler.experimental.repository.DefaultCounterRepository
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
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
    fun `invoke returns flow that emits incrementing values`() = runTest {
        val result = useCase(delayMs = 1L)
        assertIs<Result.Success<*>>(result)
        val values = result.data.take(4).toList()
        assertEquals(listOf(0L, 1L, 2L, 3L), values)
    }

    @Test
    fun `invoke stops emitting when write fails`() = runTest {
        var emitCount = 0
        val result = useCase(delayMs = 1L)
        assertIs<Result.Success<*>>(result)
        result.data.collect { value ->
            emitCount++
            if (value == 2L) {
                fakeDataSource.shouldThrowOnWrite = true
            }
        }
        // Should have emitted 0, 1, 2, then stopped after write failure on 3
        assertEquals(3, emitCount)
    }
}
