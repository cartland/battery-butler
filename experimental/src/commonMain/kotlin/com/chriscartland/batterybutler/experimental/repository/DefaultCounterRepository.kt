package com.chriscartland.batterybutler.experimental.repository

import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.experimental.datasource.LocalCounterDataSource
import com.chriscartland.batterybutler.experimental.model.CounterError
import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject
import kotlin.coroutines.cancellation.CancellationException

@Inject
class DefaultCounterRepository(
    private val localDataSource: LocalCounterDataSource,
) : CounterRepository {

    override fun observeCounter(): Flow<Long> = localDataSource.observeCounter()

    override suspend fun get(): Result<Long, CounterError> =
        try {
            Result.Success(localDataSource.getCounter())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(
                CounterError.ReadFailed(
                    message = e.message ?: "Failed to read counter",
                    cause = e.toString(),
                ),
            )
        }

    override suspend fun increment(): Result<Long, CounterError> =
        try {
            Result.Success(localDataSource.incrementCounter())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(
                CounterError.WriteFailed(
                    message = e.message ?: "Failed to increment counter",
                    cause = e.toString(),
                ),
            )
        }
}
