package com.chriscartland.batterybutler.testcommon

import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.repository.NeedsBatteryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Fake implementation of [NeedsBatteryRepository] for testing.
 *
 * Backed by a [MutableStateFlow] so tests can observe marks appearing and disappearing.
 *
 * Example usage:
 * ```kotlin
 * val repo = FakeNeedsBatteryRepository(initialFlagged = setOf("device-1"))
 * repo.setNeedsBattery("device-1", needsBattery = false)
 * assertEquals(emptyMap(), repo.getNeedsBatteryMarks().first())
 * ```
 *
 * Pass [initialMarks] instead of [initialFlagged] when the test cares *when* a device was marked
 * — the `RECENT` sort ranks on that timestamp.
 */
@OptIn(kotlin.time.ExperimentalTime::class)
class FakeNeedsBatteryRepository(
    initialFlagged: Set<String> = emptySet(),
    initialMarks: Map<String, Instant> = emptyMap(),
) : NeedsBatteryRepository {
    private val marks = MutableStateFlow(
        initialMarks + initialFlagged.filterNot { it in initialMarks }.associateWith { EPOCH_MARK },
    )

    /** Number of times [clearAll] was called, for asserting sign-out wiring. */
    var clearAllCount: Int = 0
        private set

    /** Timestamp written by [setNeedsBattery]; override to make marking deterministic. */
    var now: Instant = Clock.System.now()

    override fun getNeedsBatteryMarks(): Flow<Map<String, Instant>> = marks

    /**
     * Membership-only view, for the many tests that assert *which* devices are marked and do not
     * care when. Not part of [NeedsBatteryRepository]; production code uses
     * `GetNeedsBatteryDeviceIdsUseCase` for this.
     */
    fun getFlaggedDeviceIds(): Flow<Set<String>> = marks.map { it.keys }

    override suspend fun setNeedsBattery(
        deviceId: String,
        needsBattery: Boolean,
    ): Result<Unit, DataError> {
        marks.value = if (needsBattery) marks.value + (deviceId to now) else marks.value - deviceId
        return Result.Success(Unit)
    }

    override suspend fun clearAll(): Result<Unit, DataError> {
        clearAllCount++
        marks.value = emptyMap()
        return Result.Success(Unit)
    }

    private companion object {
        /**
         * Stand-in timestamp for a device named via [initialFlagged], which says a device is
         * marked but not when. Epoch rather than "now" so such a device sorts as least-recent
         * and cannot accidentally lead a `RECENT` assertion the test never set up.
         */
        val EPOCH_MARK = Instant.fromEpochMilliseconds(0)
    }
}
