package com.chriscartland.batterybutler.experimental.datalocal

import kotlinx.coroutines.flow.Flow

interface LocalCounterDataSource {
    fun observeCounter(): Flow<Long>

    suspend fun getCounter(): Long

    suspend fun incrementCounter(): Long
}
