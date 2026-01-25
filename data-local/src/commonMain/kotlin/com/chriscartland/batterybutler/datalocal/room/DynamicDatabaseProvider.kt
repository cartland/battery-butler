package com.chriscartland.batterybutler.datalocal.room

import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@Inject
class DynamicDatabaseProvider(
    private val factory: DatabaseFactory,
    private val networkModeRepository: NetworkModeRepository,
    private val scope: CoroutineScope,
) {
    private val initialDbName = "battery-butler.db"
    private var currentDbName: String = initialDbName
    private val _database = MutableStateFlow(factory.createDatabase(initialDbName))
    val database: StateFlow<AppDatabase> = _database.asStateFlow()

    init {
        scope.launch {
            networkModeRepository.networkMode.collect { mode ->
                val targetName = when (mode) {
                    NetworkMode.Mock -> "battery-butler-dev.db"
                    is NetworkMode.GrpcLocal -> "battery-butler.db"
                    is NetworkMode.GrpcAws -> "battery-butler.db"
                }

                if (targetName != currentDbName) {
                    val oldDb = _database.value
                    try {
                        oldDb.close()
                    } catch (e: Exception) {
                        // if (e is CancellationException) throw e
                        e.printStackTrace()
                    }
                    val newDb = factory.createDatabase(targetName)
                    _database.value = newDb
                    currentDbName = targetName
                }
            }
        }
    }
}
