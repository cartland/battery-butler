package com.chriscartland.batterybutler.data.di

import com.chriscartland.batterybutler.data.room.AppDatabase
import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.cancellation.CancellationException
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
    private var currentDbName: String = "battery-butler.db" // Default to PROD
    private val _database = MutableStateFlow(factory.createDatabase(currentDbName))
    val database: StateFlow<AppDatabase> = _database.asStateFlow()

    init {
        scope.launch {
            networkModeRepository.networkMode.collect { mode ->
                val targetName = when (mode) {
                    NetworkMode.MOCK -> "battery-butler-dev.db"
                    NetworkMode.GRPC_LOCAL -> "battery-butler.db"
                }

                if (targetName != currentDbName) {
                    val oldDb = _database.value
                    try {
                        oldDb.close()
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
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
