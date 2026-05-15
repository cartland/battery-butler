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
 * **Restore caveat (bb-lg42).** Even with the [rebindSignal] mechanism below,
 * in-process Flow consumers can fail to re-bind after [restoreFromLegacy] —
 * observed on android/32 where Devices / Types / History all required an app
 * process restart to see restored data. The pragmatic fix lives in the UI:
 * `SettingsScreen` triggers an [com.chriscartland.batterybutler.presentationcore.util.AppRestarter]
 * after a [RestoreResult.Success] / [RestoreResult.DestructiveFallback]
 * snackbar. This file is intentionally kept simple (direct overwrite,
 * minimal state changes) because the in-process rebind path is no longer the
 * trusted code path — the trusted path is "swap then restart". The rebind
 * signal remains as defense-in-depth for any consumer that happens to be
 * active and listening.
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

    private val _rebindSignal = MutableSharedFlow<Long>(replay = 0, extraBufferCapacity = 1)
    val rebindSignal: SharedFlow<Long> = _rebindSignal.asSharedFlow()
    private var rebindCounter: Long = 0L

    init {
        scope.launch {
            networkModeRepository.networkMode.collect { mode ->
                val targetOption = DatabaseOption.fromNetworkMode(mode)

                switchMutex.withLock {
                    if (targetOption != currentOption) {
                        val newDb = factory.createDatabase(targetOption)
                        val oldDb = _database.value
                        val previousOption = currentOption

                        _database.value = newDb
                        currentOption = targetOption

                        try {
                            oldDb.close()
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            log.w { "Failed to close old database: ${e.message}" }
                        }

                        factory.evict(previousOption)
                    }
                }
            }
        }
    }

    /**
     * Restores the current database option from its legacy (tag 27) file.
     * Direct overwrite — does NOT use atomic backup-then-restore (that was
     * tried in android/32 and made the Devices flow regress, see bb-lg42).
     *
     * After this method returns successfully the caller should restart the
     * app process via [com.chriscartland.batterybutler.presentationcore.util.AppRestarter].
     * The provider's in-process [database] StateFlow IS updated and the
     * [rebindSignal] IS emitted as a best-effort signal for any active
     * Flow consumer, but those signals are not the trusted recovery path;
     * the process restart is.
     */
    suspend fun restoreFromLegacy(legacyFileName: String): RestoreResult {
        switchMutex.withLock {
            val option = currentOption
            val activeFileName = option.fileName

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

            try {
                factory.copyDatabaseFile(legacyFileName, activeFileName)
                log.i { "Copied legacy file $legacyFileName -> $activeFileName (direct overwrite)" }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                log.e(e) { "Failed to copy legacy file" }
                // Reopen the same path (the active file may be partially overwritten).
                val reopened = factory.createDatabase(option)
                _database.value = reopened
                return RestoreResult.Failure(
                    errorMessage = "Copy failed: ${e.message}",
                    throwableClassName = e::class.simpleName ?: "Throwable",
                )
            }

            // Reopen Room from the (now restored) file.
            val newDb = factory.createDatabase(option)
            _database.value = newDb

            rebindCounter += 1L
            _rebindSignal.tryEmit(rebindCounter)
            log.i { "Restore complete: rebindCounter=$rebindCounter" }

            // Surface destructive-fallback to the UI when the legacy file was at
            // a schema version that fallbackToDestructiveMigrationFrom(0,1,2) will
            // wipe instead of migrate. The actual fallback runs lazily on the
            // first query against newDb; we signal eagerly so the snackbar can
            // warn before the app restart kicks in.
            return if (legacyUserVersion in 0..2) {
                log.w { "Destructive fallback expected (legacy user_version=$legacyUserVersion); data will NOT be recovered" }
                RestoreResult.DestructiveFallback(fromVersion = legacyUserVersion)
            } else {
                RestoreResult.Success
            }
        }
    }
}
