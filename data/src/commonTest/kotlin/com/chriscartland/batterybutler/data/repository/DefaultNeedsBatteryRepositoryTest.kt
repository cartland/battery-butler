package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.datalocal.NeedsBatteryStore
import com.chriscartland.batterybutler.domain.model.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DefaultNeedsBatteryRepositoryTest {
    private class RecordingStore(
        initial: Set<String> = emptySet(),
    ) : NeedsBatteryStore {
        val flagged = MutableStateFlow(initial)
        var lastFlaggedAt: Long? = null
        var clearAllCount = 0
        var throwOnWrite: Boolean = false

        override fun observeFlaggedDeviceIds(): Flow<Set<String>> = flagged

        override suspend fun flag(
            deviceId: String,
            flaggedAt: Long,
        ) {
            if (throwOnWrite) throw IllegalStateException("disk full")
            lastFlaggedAt = flaggedAt
            flagged.value = flagged.value + deviceId
        }

        override suspend fun clear(deviceId: String) {
            if (throwOnWrite) throw IllegalStateException("disk full")
            flagged.value = flagged.value - deviceId
        }

        override suspend fun clearAll() {
            clearAllCount++
            flagged.value = emptySet()
        }
    }

    @Test
    fun `setting a mark writes through to the store`() =
        runTest {
            val store = RecordingStore()
            val repo = DefaultNeedsBatteryRepository(store)

            val result = repo.setNeedsBattery("d1", needsBattery = true)

            assertIs<Result.Success<Unit>>(result)
            assertEquals(setOf("d1"), store.flagged.value)
        }

    @Test
    fun `clearing a mark removes it from the store`() =
        runTest {
            val store = RecordingStore(initial = setOf("d1"))
            val repo = DefaultNeedsBatteryRepository(store)

            repo.setNeedsBattery("d1", needsBattery = false)

            assertTrue(store.flagged.value.isEmpty())
        }

    @Test
    fun `marking records when it happened`() =
        runTest {
            val store = RecordingStore()

            DefaultNeedsBatteryRepository(store).setNeedsBattery("d1", needsBattery = true)

            assertTrue((store.lastFlaggedAt ?: 0L) > 0L)
        }

    @Test
    fun `flagged ids are read straight from the store`() =
        runTest {
            val store = RecordingStore(initial = setOf("d1", "d2"))

            assertEquals(setOf("d1", "d2"), DefaultNeedsBatteryRepository(store).getFlaggedDeviceIds().first())
        }

    @Test
    fun `clearAll empties the store`() =
        runTest {
            val store = RecordingStore(initial = setOf("d1", "d2"))

            DefaultNeedsBatteryRepository(store).clearAll()

            assertEquals(1, store.clearAllCount)
            assertTrue(store.flagged.value.isEmpty())
        }

    /** A failed write must surface as an error rather than throwing out of a UI callback. */
    @Test
    fun `a store failure comes back as an error result`() =
        runTest {
            val store = RecordingStore()
            store.throwOnWrite = true

            val result = DefaultNeedsBatteryRepository(store).setNeedsBattery("d1", needsBattery = true)

            assertIs<Result.Error<*>>(result)
        }
}
