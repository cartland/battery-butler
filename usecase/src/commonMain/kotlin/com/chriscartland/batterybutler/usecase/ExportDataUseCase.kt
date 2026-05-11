package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.DispatcherProvider
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject

@Inject
class ExportDataUseCase(
    private val deviceRepository: DeviceRepository,
    private val dispatcherProvider: DispatcherProvider,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend operator fun invoke(): String =
        withContext(dispatcherProvider.default) {
            val devices = deviceRepository.getAllDevices().first()
            val types = deviceRepository.getAllDeviceTypes().first()
            val events = deviceRepository.getAllEvents().first()

            val exportData = BackupData(
                devices = devices.map { it.toDto() },
                deviceTypes = types.map { it.toDto() },
                events = events.map { it.toDto() },
            )

            val container = BackupContainer(
                data = exportData,
            )

            json.encodeToString(container)
        }
}
