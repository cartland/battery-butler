package com.chriscartland.batterybutler.experimental.datasource

import kotlinx.coroutines.flow.Flow

interface LocalCounterDataSource {
    fun observeCounter(): Flow<Long>
    suspend fun getCounter(): Long
    suspend fun setCounter(value: Long)
}
