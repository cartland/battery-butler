package com.chriscartland.batterybutler.experimental.usecase

import com.chriscartland.batterybutler.experimental.data.DefaultCounterRepository
import com.chriscartland.batterybutler.experimental.datalocal.FakeLocalCounterDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveCounterUseCaseTest {
    private lateinit var fakeDataSource: FakeLocalCounterDataSource
    private lateinit var useCase: ObserveCounterUseCase

    @BeforeTest
    fun setUp() {
        fakeDataSource = FakeLocalCounterDataSource()
        useCase = ObserveCounterUseCase(DefaultCounterRepository(fakeDataSource))
    }

    @Test
    fun `invoke returns flow that emits current counter value`() =
        runTest {
            val value = useCase().first()
            assertEquals(0L, value)
        }

    @Test
    fun `invoke emits updated values after increment`() =
        runTest {
            fakeDataSource.incrementCounter()
            fakeDataSource.incrementCounter()
            val value = useCase().first()
            assertEquals(2L, value)
        }
}
