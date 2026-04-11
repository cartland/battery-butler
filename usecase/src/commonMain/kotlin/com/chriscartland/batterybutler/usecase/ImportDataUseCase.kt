package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.DispatcherProvider
import com.chriscartland.batterybutler.domain.model.ImportResult
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject

@Inject
class ImportDataUseCase(
    private val deviceRepository: DeviceRepository,
    private val dispatcherProvider: DispatcherProvider,
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend operator fun invoke(jsonString: String): Result<ImportResult, DataError> =
        withContext(dispatcherProvider.default) {
            // Step 1: Parse envelope to check format version and type.
            val envelope = try {
                json.decodeFromString<BackupEnvelope>(jsonString)
            } catch (e: Exception) {
                return@withContext Result.Error(
                    DataError.Database.ReadFailed(
                        message = "Invalid backup file: could not parse JSON.",
                        cause = e.message,
                    ),
                )
            }

            if (envelope.type != BACKUP_TYPE) {
                return@withContext Result.Error(
                    DataError.Database.ReadFailed(
                        message = "Invalid backup file: expected type \"$BACKUP_TYPE\" but got \"${envelope.type}\".",
                    ),
                )
            }

            if (envelope.formatVersion != BACKUP_FORMAT_VERSION) {
                return@withContext Result.Error(
                    DataError.Database.ReadFailed(
                        message = "Unsupported backup version ${envelope.formatVersion}. Please update the app.",
                    ),
                )
            }

            // Step 2: Parse full container for v1.
            val container = try {
                json.decodeFromString<BackupContainer>(jsonString)
            } catch (e: Exception) {
                return@withContext Result.Error(
                    DataError.Database.ReadFailed(
                        message = "Invalid backup file: could not parse data.",
                        cause = e.message,
                    ),
                )
            }

            importV1(container.data)
        }

    private suspend fun importV1(data: BackupData): Result<ImportResult, DataError> {
        val errors = mutableListOf<String>()
        var deviceTypesImported = 0
        var devicesImported = 0
        var eventsImported = 0

        // Import device types first (devices reference them).
        for (dto in data.deviceTypes) {
            try {
                val domainType = dto.toDomain()
                when (val result = deviceRepository.addDeviceType(domainType)) {
                    is Result.Success -> deviceTypesImported++
                    is Result.Error -> errors.add("DeviceType ${dto.id}: ${result.error.message}")
                }
            } catch (e: Exception) {
                errors.add("DeviceType ${dto.id}: ${e.message}")
            }
        }

        // Import devices (events reference them).
        for (dto in data.devices) {
            try {
                val domainDevice = dto.toDomain()
                when (val result = deviceRepository.addDevice(domainDevice)) {
                    is Result.Success -> devicesImported++
                    is Result.Error -> errors.add("Device ${dto.id}: ${result.error.message}")
                }
            } catch (e: Exception) {
                errors.add("Device ${dto.id}: ${e.message}")
            }
        }

        // Import events last.
        for (dto in data.events) {
            try {
                val domainEvent = dto.toDomain()
                when (val result = deviceRepository.addEvent(domainEvent)) {
                    is Result.Success -> eventsImported++
                    is Result.Error -> errors.add("Event ${dto.id}: ${result.error.message}")
                }
            } catch (e: Exception) {
                errors.add("Event ${dto.id}: ${e.message}")
            }
        }

        return Result.Success(
            ImportResult(
                devicesImported = devicesImported,
                deviceTypesImported = deviceTypesImported,
                eventsImported = eventsImported,
                errors = errors,
            ),
        )
    }
}

/**
 * Minimal envelope for parsing only the version and type fields
 * before committing to a full parse.
 */
@Serializable
internal data class BackupEnvelope(
    val formatVersion: Int = BACKUP_FORMAT_VERSION,
    val type: String = BACKUP_TYPE,
)
