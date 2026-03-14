package com.chriscartland.batterybutler.experimental.usecase

import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.experimental.datalocal.DefaultCounterRepository
import com.chriscartland.batterybutler.experimental.datalocal.FakeLocalCounterDataSource
import com.chriscartland.batterybutler.experimental.domain.model.CounterError
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class IncrementCounterUseCaseTest {
    private lateinit var fakeDataSource: FakeLocalCounterDataSource
    private lateinit var useCase: IncrementCounterUseCase

    @BeforeTest
    fun setUp() {
        fakeDataSource = FakeLocalCounterDataSource()
        useCase = IncrementCounterUseCase(DefaultCounterRepository(fakeDataSource))
    }

    @Test
    fun `invoke returns success with incremented value`() =
        runTest {
            val result = useCase()
            assertIs<Result.Success<Long>>(result)
            assertEquals(1L, result.data)
        }

    @Test
    fun `invoke increments sequentially`() =
        runTest {
            useCase()
            useCase()
            val result = useCase()
            assertIs<Result.Success<Long>>(result)
            assertEquals(3L, result.data)
        }

    @Test
    fun `invoke returns error when write fails`() =
        runTest {
            fakeDataSource.setWriteError(true)
            val result = useCase()
            assertIs<Result.Error<CounterError>>(result)
            assertIs<CounterError.WriteFailed>(result.error)
        }
}
