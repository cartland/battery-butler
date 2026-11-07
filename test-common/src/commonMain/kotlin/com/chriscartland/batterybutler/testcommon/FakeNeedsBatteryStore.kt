package com.chriscartland.batterybutler.testcommon

import com.chriscartland.batterybutler.datalocal.NeedsBatteryStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Instant

class FakeNeedsBatteryStore : NeedsBatteryStore {
    val flags = MutableStateFlow<Map<String, Instant>>(emptyMap())

    override fun observeFlags(): Flow<Map<String, Instant>> = flags

    override suspend fun flag(
        deviceId: String,
        flaggedAt: Long,
    ) {
        flags.value = flags.value + (deviceId to Instant.fromEpochMilliseconds(flaggedAt))
    }

    override suspend fun clear(deviceId: String) {
        flags.value = flags.value - deviceId
    }

    override suspend fun clearAll() {
        flags.value = emptyMap()
    }
}
