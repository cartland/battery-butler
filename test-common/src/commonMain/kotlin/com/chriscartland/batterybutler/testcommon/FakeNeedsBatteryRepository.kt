package com.chriscartland.batterybutler.testcommon

import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.repository.NeedsBatteryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake implementation of [NeedsBatteryRepository] for testing.
 *
 * Backed by a [MutableStateFlow] so tests can observe marks appearing and disappearing.
 *
 * Example usage:
 * ```kotlin
 * val repo = FakeNeedsBatteryRepository(initialFlagged = setOf("device-1"))
 * repo.setNeedsBattery("device-1", needsBattery = false)
 * assertEquals(emptySet(), repo.getFlaggedDeviceIds().first())
 * ```
 */
class FakeNeedsBatteryRepository(
    initialFlagged: Set<String> = emptySet(),
) : NeedsBatteryRepository {
    private val flagged = MutableStateFlow(initialFlagged)

    /** Number of times [clearAll] was called, for asserting sign-out wiring. */
    var clearAllCount: Int = 0
        private set

    override fun getFlaggedDeviceIds(): Flow<Set<String>> = flagged

    override suspend fun setNeedsBattery(
        deviceId: String,
        needsBattery: Boolean,
    ): Result<Unit, DataError> {
        flagged.value = if (needsBattery) flagged.value + deviceId else flagged.value - deviceId
        return Result.Success(Unit)
    }

    override suspend fun clearAll(): Result<Unit, DataError> {
        clearAllCount++
        flagged.value = emptySet()
        return Result.Success(Unit)
    }
}
