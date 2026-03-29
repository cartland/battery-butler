package com.chriscartland.batterybutler.datalocal.room

import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.tatarka.inject.annotations.Inject

/**
 * Provides a dynamically switchable database based on the current network mode.
 *
 * Uses a mutex to ensure atomic database switching - external code reading [database]
 * will never observe a closed database during mode transitions.
 */
@Inject
class DynamicDatabaseProvider(
    private val factory: DatabaseFactory,
    private val networkModeRepository: NetworkModeRepository,
    private val scope: CoroutineScope,
) {
    private val switchMutex = Mutex()
    private var currentOption: DatabaseOption = DatabaseOption.Offline
    private val _database = MutableStateFlow(factory.createDatabase(DatabaseOption.Offline))
    val database: StateFlow<AppDatabase> = _database.asStateFlow()

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
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            co.touchlab.kermit.Logger.w("DynamicDatabaseProvider") {
                                "Failed to close old database: ${e.message}"
                            }
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
     * Restores the current database option from its legacy (tag 27) file.
     * Closes the current DB, copies legacy file over current file, then reopens.
     */
    suspend fun restoreFromLegacy(legacyFileName: String) {
        switchMutex.withLock {
            val oldDb = _database.value
            val option = currentOption

            // Close current database so file is not locked
            try {
                oldDb.close()
            } catch (_: Exception) {
            }
            factory.evict(option)

            // Copy legacy file over current file
            factory.copyDatabaseFile(legacyFileName, option.fileName)

            // Reopen database from the restored file
            val newDb = factory.createDatabase(option)
            _database.value = newDb
        }
    }
}
