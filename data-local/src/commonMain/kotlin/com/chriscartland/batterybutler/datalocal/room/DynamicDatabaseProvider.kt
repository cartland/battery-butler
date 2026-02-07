package com.chriscartland.batterybutler.datalocal.room

import com.chriscartland.batterybutler.domain.model.NetworkMode
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
    private var currentDbName: String = DatabaseConstants.PRODUCTION_DATABASE_NAME
    private val _database = MutableStateFlow(factory.createDatabase(DatabaseConstants.PRODUCTION_DATABASE_NAME))
    val database: StateFlow<AppDatabase> = _database.asStateFlow()

    init {
        scope.launch {
            networkModeRepository.networkMode.collect { mode ->
                val targetName = when (mode) {
                    NetworkMode.Mock -> DatabaseConstants.DEVELOPMENT_DATABASE_NAME
                    is NetworkMode.GrpcLocal,
                    is NetworkMode.GrpcAws,
                    -> DatabaseConstants.PRODUCTION_DATABASE_NAME
                }

                switchMutex.withLock {
                    if (targetName != currentDbName) {
                        // Create new database BEFORE closing old one to minimize
                        // window where no valid database is available
                        val newDb = factory.createDatabase(targetName)
                        val oldDb = _database.value

                        // Update state atomically: new database first, then name
                        _database.value = newDb
                        currentDbName = targetName

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
                    }
                }
            }
        }
    }
}
