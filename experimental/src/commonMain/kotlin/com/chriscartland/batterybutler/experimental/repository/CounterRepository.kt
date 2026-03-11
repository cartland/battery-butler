package com.chriscartland.batterybutler.experimental.repository

import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.experimental.model.CounterError
import kotlinx.coroutines.flow.Flow

interface CounterRepository {
    fun observeCounter(): Flow<Long>
    suspend fun get(): Result<Long, CounterError>
    suspend fun increment(): Result<Long, CounterError>
}
