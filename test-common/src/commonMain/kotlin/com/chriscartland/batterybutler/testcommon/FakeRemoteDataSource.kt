package com.chriscartland.batterybutler.testcommon

import com.chriscartland.batterybutler.datanetwork.RemoteDataSource
import com.chriscartland.batterybutler.datanetwork.RemoteDataSourceState
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

/**
 * Fake implementation of [RemoteDataSource] for testing.
 *
 * Provides controllable push behavior:
 * - [shouldFail] makes [push] return false
 * - [suspendPush] + [resumePush] allow capturing intermediate states
 * - [pushedUpdates] records all push calls
 *
 * Example usage:
 * ```kotlin
 * val remote = FakeRemoteDataSource()
 * remote.shouldFail = true
 * repo.addDevice(device)
 * assertEquals(SyncStatus.Failed, repo.syncStatus.value)
 * ```
 */
class FakeRemoteDataSource : RemoteDataSource {
    val pushedUpdates = mutableListOf<RemoteUpdate>()
    var shouldFail = false
    var suspendPush = false
    private var pushDeferred: CompletableDeferred<Unit>? = null

    override val state: StateFlow<RemoteDataSourceState> =
        MutableStateFlow(RemoteDataSourceState.NotStarted)

    override fun subscribe(): Flow<RemoteUpdate> = flow { awaitCancellation() }

    override suspend fun push(update: RemoteUpdate): Boolean {
        pushedUpdates.add(update)
        if (suspendPush) {
            pushDeferred = CompletableDeferred()
            pushDeferred?.await()
        }
        return !shouldFail
    }

    fun resumePush() {
        pushDeferred?.complete(Unit)
    }
}
