package com.chriscartland.batterybutler.testcommon

import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.repository.DataModeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake implementation of [DataModeRepository] for testing.
 *
 * Backed by a [MutableStateFlow] for reactive updates.
 *
 * Example usage:
 * ```kotlin
 * val repo = FakeDataModeRepository()
 * repo.setDataMode(DataMode.Mock)
 * assertEquals(DataMode.Mock, repo.dataMode.first())
 * ```
 */
class FakeDataModeRepository(
    initialMode: DataMode = DataMode.None,
) : DataModeRepository {
    private val _dataMode = MutableStateFlow(initialMode)

    override val dataMode: Flow<DataMode> = _dataMode

    override suspend fun setDataMode(mode: DataMode) {
        _dataMode.value = mode
    }
}
