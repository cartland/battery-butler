package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.datalocal.NeedsBatteryStore
import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.repository.NeedsBatteryRepository
import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Local-only, so there is no [SyncManager] here and nothing is ever pushed -- the mark stays on
 * this device by design.
 */
@OptIn(ExperimentalTime::class)
@Inject
class DefaultNeedsBatteryRepository(
    private val store: NeedsBatteryStore,
) : NeedsBatteryRepository {
    override fun getFlaggedDeviceIds(): Flow<Set<String>> = store.observeFlaggedDeviceIds()

    override suspend fun setNeedsBattery(
        deviceId: String,
        needsBattery: Boolean,
    ): Result<Unit, DataError> =
        runWriteOperation("Failed to update battery mark") {
            if (needsBattery) {
                store.flag(deviceId, Clock.System.now().toEpochMilliseconds())
            } else {
                store.clear(deviceId)
            }
        }

    override suspend fun clearAll(): Result<Unit, DataError> = runWriteOperation("Failed to clear battery marks") { store.clearAll() }

    private suspend inline fun runWriteOperation(
        failureMessage: String,
        crossinline block: suspend () -> Unit,
    ): Result<Unit, DataError> =
        try {
            block()
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(
                DataError.Database.WriteFailed(
                    message = e.message ?: failureMessage,
                    cause = e.toString(),
                ),
            )
        }
}
