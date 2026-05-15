package com.chriscartland.batterybutler.datalocal.room

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.domain.model.RestoreResult
import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.tatarka.inject.annotations.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Provides a dynamically switchable database based on the current network mode.
 *
 * Uses a mutex to ensure atomic database switching - external code reading [database]
 * will never observe a closed database during mode transitions.
 *
 * Consumers that produce a long-lived `Flow` derived from `database.flatMapLatest`
 * should ALSO observe [rebindSignal] (e.g. `combine(database, rebindSignal.onStart { emit(0L) })`).
 * The StateFlow swap alone is not always sufficient to guarantee downstream Room
 * `@Query` Flows re-emit after [restoreFromLegacy] — observed in bd issue bb-lg42
 * where DeviceTypes / History tabs stayed stuck on `Loading` until app restart even
 * though the StateFlow had emitted the new database. Emitting on [rebindSignal] after
 * each restore forces a fresh `flatMapLatest` re-subscription.
 */
@Inject
class DynamicDatabaseProvider(
    private val factory: DatabaseFactory,
    private val networkModeRepository: NetworkModeRepository,
    private val scope: CoroutineScope,
) {
    private val log = Logger.withTag("DynamicDbProvider")
    private val switchMutex = Mutex()
    private var currentOption: DatabaseOption = DatabaseOption.Offline
    private val _database = MutableStateFlow(factory.createDatabase(DatabaseOption.Offline))
    val database: StateFlow<AppDatabase> = _database.asStateFlow()

    /**
     * Monotonically-increasing counter that ticks every time the underlying database
     * file is replaced (currently only on [restoreFromLegacy]). Consumers of [database]
     * should `combine` with this so the active `flatMapLatest` rebinds on restore.
     *
     * `extraBufferCapacity = 1` so a `tryEmit` from inside `switchMutex.withLock` is
     * non-blocking even if no collector has consumed the previous tick yet.
     */
    private val _rebindSignal = MutableSharedFlow<Long>(replay = 0, extraBufferCapacity = 1)
    val rebindSignal: SharedFlow<Long> = _rebindSignal.asSharedFlow()
    private var rebindCounter: Long = 0L

    init {
        scope.launch {
            networkModeRepository.networkMode.collect { mode ->
                val targetOption = DatabaseOption.fromNetworkMode(mode)

                switchMutex.withLock {
                    if (targetOption != currentOption) {
                        // Create new database BEFORE closing old one to minimize
                        // window where no valid database is available
                        val newDb = factory.createDatabase(targetOption)
                        val oldDb = _database.value
                        val previousOption = currentOption

                        // Update state atomically: new database first, then option
                        _database.value = newDb
                        currentOption = targetOption

                        // Close old database AFTER switching to prevent
                        // external code from accessing closed database
                        try {
                            oldDb.close()
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            log.w { "Failed to close old database: ${e.message}" }
                        }

                        // Evict the closed instance from the cache so a fresh
                        // instance is created if this option is requested again
                        factory.evict(previousOption)
                    }
                }
            }
        }
    }

    /**
     * Restores the current database option from its legacy (tag 27) file using an
     * atomic-swap pattern: the active database file is renamed to a `.bak` backup
     * BEFORE the legacy file is copied over the active filename. If anything fails
     * — copy IO error, Room open error, migration error — the backup is renamed
     * back and the provider returns the original active database. The user-visible
     * outcome is captured in [RestoreResult]; this method does not throw.
     *
     * Diagnostic instrumentation: every phase is logged via Kermit at INFO level so
     * release-build users can capture logcat traces.
     *
     * Closes [bb-lg42]: previous implementation left the active DB in a corrupted
     * half-state if the copy or open step failed, with no observable signal to the UI.
     */
    suspend fun restoreFromLegacy(legacyFileName: String): RestoreResult {
        switchMutex.withLock {
            val option = currentOption
            val activeFileName = option.fileName
            val backupFileName = "$activeFileName.bak"

            log.i { "restoreFromLegacy: option=$option legacy=$legacyFileName active=$activeFileName" }

            if (!factory.databaseFileExists(legacyFileName)) {
                log.w { "Legacy file not found: $legacyFileName" }
                return RestoreResult.LegacyFileUnavailable(reason = "Legacy file does not exist: $legacyFileName")
            }

            val legacyUserVersion = factory.readUserVersion(legacyFileName)
            log.i { "Legacy file user_version=$legacyUserVersion" }

            val oldDb = _database.value
            try {
                oldDb.close()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                log.w { "Failed to close active database before restore: ${e.message}" }
            }
            factory.evict(option)

            // Step 1: back up active file (if any).
            val hadActive = factory.databaseFileExists(activeFileName)
            if (hadActive) {
                try {
                    factory.renameDatabaseFile(activeFileName, backupFileName)
                    log.i { "Backed up $activeFileName -> $backupFileName" }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    log.e(e) { "Failed to back up active DB before restore" }
                    // Reopen the original DB so the provider is not in a broken state.
                    return reopenAndFail(option, "Backup step failed: ${e.message}", e)
                }
            }

            // Step 2: copy legacy file into the active filename.
            try {
                factory.copyDatabaseFile(legacyFileName, activeFileName)
                log.i { "Copied legacy file $legacyFileName -> $activeFileName" }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                log.e(e) { "Failed to copy legacy file" }
                // Roll back the backup.
                if (hadActive) {
                    runCatching { factory.renameDatabaseFile(backupFileName, activeFileName) }
                        .onFailure { ex -> log.e(ex) { "Backup rollback also failed" } }
                }
                return reopenAndFail(option, "Copy failed: ${e.message}", e)
            }

            val restoredUserVersion = factory.readUserVersion(activeFileName)
            log.i { "Restored active file user_version=$restoredUserVersion" }

            // Step 3: open Room against the restored file. Forces validation +
            // migrations to run synchronously so we can react to failures.
            val newDb = factory.createDatabase(option)
            val openOutcome = runCatching {
                // Touching openHelper.writableConnection.use isn't available in KMP Room;
                // running a trivial query against the dao forces the database to open
                // and migrations to apply on this thread.
                newDb.deviceDao()
            }
            if (openOutcome.isFailure) {
                val e = openOutcome.exceptionOrNull() ?: RuntimeException("unknown open failure")
                if (e is CancellationException) throw e
                log.e(e) { "Failed to open restored Room database" }
                try {
                    newDb.close()
                } catch (closeErr: Exception) {
                    if (closeErr is CancellationException) throw closeErr
                    log.w { "Failed to close failed-to-open Room instance: ${closeErr.message}" }
                }
                factory.evict(option)
                // Roll back: remove the (broken) restored file, restore backup.
                runCatching { factory.deleteDatabaseFile(activeFileName) }
                if (hadActive) {
                    runCatching { factory.renameDatabaseFile(backupFileName, activeFileName) }
                        .onFailure { ex -> log.e(ex) { "Backup rollback failed after Room open failure" } }
                }
                return reopenAndFail(option, "Room open failed: ${e.message}", e)
            }

            // Success path: clean up backup, swap StateFlow, tick rebind signal.
            if (hadActive) {
                runCatching { factory.deleteDatabaseFile(backupFileName) }
                    .onFailure { ex -> log.w(ex) { "Failed to delete restore backup; non-fatal" } }
            }
            _database.value = newDb
            rebindCounter += 1L
            _rebindSignal.tryEmit(rebindCounter)
            log.i { "Restore complete: rebindCounter=$rebindCounter" }

            // If the user_version on the restored file was outside the migration
            // path (Room had to fall back to a fresh schema), signal that to the UI.
            return if (legacyUserVersion in 0..2) {
                log.w { "Destructive fallback applied (legacy user_version=$legacyUserVersion); data was NOT recovered" }
                RestoreResult.DestructiveFallback(fromVersion = legacyUserVersion)
            } else {
                RestoreResult.Success
            }
        }
    }

    /**
     * Internal helper: re-open the current option after a failed restore so the
     * provider has a working AppDatabase, and return a Failure result.
     */
    private fun reopenAndFail(
        option: DatabaseOption,
        message: String,
        cause: Throwable,
    ): RestoreResult {
        val reopened = factory.createDatabase(option)
        _database.value = reopened
        rebindCounter += 1L
        _rebindSignal.tryEmit(rebindCounter)
        log.i { "Reopened active DB after failure; rebindCounter=$rebindCounter" }
        return RestoreResult.Failure(
            errorMessage = message,
            throwableClassName = cause::class.simpleName ?: "Throwable",
        )
    }
}
