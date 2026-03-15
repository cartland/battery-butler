package com.chriscartland.batterybutler.experimental.usecase

import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.experimental.data.DefaultCounterRepository
import com.chriscartland.batterybutler.experimental.datalocal.FakeLocalCounterDataSource
import com.chriscartland.batterybutler.experimental.domain.model.CounterError
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GetCounterUseCaseTest {
    private lateinit var fakeDataSource: FakeLocalCounterDataSource
    private lateinit var useCase: GetCounterUseCase

    @BeforeTest
    fun setUp() {
        fakeDataSource = FakeLocalCounterDataSource()
        useCase = GetCounterUseCase(DefaultCounterRepository(fakeDataSource))
    }

    @Test
    fun `invoke returns success with current counter value`() =
        runTest {
            fakeDataSource.incrementCounter()
            fakeDataSource.incrementCounter()
            fakeDataSource.incrementCounter()
            val result = useCase()
            assertIs<Result.Success<Long>>(result)
            assertEquals(3L, result.data)
        }

    @Test
    fun `invoke returns error when read fails`() =
        runTest {
            fakeDataSource.setReadError(true)
            val result = useCase()
            assertIs<Result.Error<CounterError>>(result)
            assertIs<CounterError.ReadFailed>(result.error)
        }
}
