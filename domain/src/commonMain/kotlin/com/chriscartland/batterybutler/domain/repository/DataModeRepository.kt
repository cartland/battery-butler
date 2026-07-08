package com.chriscartland.batterybutler.domain.repository

import com.chriscartland.batterybutler.domain.model.DataMode
import kotlinx.coroutines.flow.Flow

interface DataModeRepository {
    val dataMode: Flow<DataMode>

    suspend fun setDataMode(mode: DataMode)
}
