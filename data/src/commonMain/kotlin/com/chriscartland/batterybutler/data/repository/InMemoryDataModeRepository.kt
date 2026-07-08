package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.repository.DataModeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Inject

@Inject
class InMemoryDataModeRepository : DataModeRepository {
    private val _dataMode = MutableStateFlow<DataMode>(DataMode.Mock)
    override val dataMode: Flow<DataMode> = _dataMode.asStateFlow()

    override suspend fun setDataMode(mode: DataMode) {
        _dataMode.value = mode
    }
}
