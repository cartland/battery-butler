package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.DispatcherProvider
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.SyncStatus
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class ImportDataUseCaseTest {
    private val testDispatcherProvider = object : DispatcherProvider {
        private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
        override val default: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val main: CoroutineDispatcher = dispatcher
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private fun makeValidJson(
        devices: List<DeviceDto> = emptyList(),
        deviceTypes: List<DeviceTypeDto> = emptyList(),
        events: List<BatteryEventDto> = emptyList(),
        formatVersion: Int = BACKUP_FORMAT_VERSION,
        type: String = BACKUP_TYPE,
    ): String =
        json.encodeToString(
            BackupContainer(
                formatVersion = formatVersion,
                type = type,
                data = BackupData(
                    devices = devices,
                    deviceTypes = deviceTypes,
                    events = events,
                ),
            ),
        )

    // region Happy path tests

    @Test
    fun `import valid v1 JSON imports all records with correct counts`() =
        runTest {
            val repository = TestImportRepository()
            val useCase = ImportDataUseCase(repository, testDispatcherProvider)

            val jsonString = makeValidJson(
                deviceTypes = listOf(
                    DeviceTypeDto("t1", "Smoke Detector", "9V", 1, "detector_smoke"),
                ),
                devices = listOf(
                    DeviceDto("d1", "Kitchen Alarm", "t1", "2024-01-15T10:30:00Z", "Kitchen"),
                    DeviceDto("d2", "Hallway Alarm", "t1", "2024-02-20T08:00:00Z", null),
                ),
                events = listOf(
                    BatteryEventDto("e1", "d1", "2024-01-15T10:30:00Z", "9V", "Initial install"),
                    BatteryEventDto("e2", "d2", "2024-02-20T08:00:00Z", null, null),
                    BatteryEventDto("e3", "d1", "2024-06-15T12:00:00Z", "9V", null),
                ),
            )

            val result = useCase(jsonString)

            assertTrue(result is Result.Success)
            val importResult = (result as Result.Success).data
            assertEquals(1, importResult.deviceTypesImported)
            assertEquals(2, importResult.devicesImported)
            assertEquals(3, importResult.eventsImported)
            assertTrue(importResult.errors.isEmpty())
        }

    @Test
    fun `import preserves all device fields`() =
        runTest {
            val repository = TestImportRepository()
            val useCase = ImportDataUseCase(repository, testDispatcherProvider)

            val jsonString = makeValidJson(
                devices = listOf(
                    DeviceDto("d1", "Kitchen Alarm", "t1", "2024-06-15T10:30:00Z", "Kitchen"),
                ),
            )

            useCase(jsonString)

            val device = repository.devices.first()
            assertEquals("d1", device.id)
            assertEquals("Kitchen Alarm", device.name)
            assertEquals("t1", device.typeId)
            assertEquals(Instant.parse("2024-06-15T10:30:00Z"), device.batteryLastReplaced)
            assertEquals("Kitchen", device.location)
        }

    @Test
    fun `import preserves all device type fields`() =
        runTest {
            val repository = TestImportRepository()
            val useCase = ImportDataUseCase(repository, testDispatcherProvider)

            val jsonString = makeValidJson(
                deviceTypes = listOf(
                    DeviceTypeDto("t1", "Smoke Detector", "9V", 2, "detector_smoke"),
                ),
            )

            useCase(jsonString)

            val type = repository.deviceTypes.first()
            assertEquals("t1", type.id)
            assertEquals("Smoke Detector", type.name)
            assertEquals("9V", type.batteryType)
            assertEquals(2, type.batteryQuantity)
            assertEquals("detector_smoke", type.defaultIcon)
        }

    @Test
    fun `import preserves all event fields`() =
        runTest {
            val repository = TestImportRepository()
            val useCase = ImportDataUseCase(repository, testDispatcherProvider)

            val jsonString = makeValidJson(
                events = listOf(
                    BatteryEventDto("e1", "d1", "2024-06-20T14:00:00Z", "AA", "Test note"),
                ),
            )

            useCase(jsonString)

            val event = repository.events.first()
            assertEquals("e1", event.id)
            assertEquals("d1", event.deviceId)
            assertEquals(Instant.parse("2024-06-20T14:00:00Z"), event.date)
            assertEquals("AA", event.batteryType)
            assertEquals("Test note", event.notes)
        }

    @Test
    fun `import empty data sections returns zero counts`() =
        runTest {
            val repository = TestImportRepository()
            val useCase = ImportDataUseCase(repository, testDispatcherProvider)

            val jsonString = makeValidJson()

            val result = useCase(jsonString)

            assertTrue(result is Result.Success)
            val importResult = (result as Result.Success).data
            assertEquals(0, importResult.devicesImported)
            assertEquals(0, importResult.deviceTypesImported)
            assertEquals(0, importResult.eventsImported)
            assertTrue(importResult.errors.isEmpty())
        }

    @Test
    fun `import handles null optional fields`() =
        runTest {
            val repository = TestImportRepository()
            val useCase = ImportDataUseCase(repository, testDispatcherProvider)

            val jsonString = makeValidJson(
                devices = listOf(
                    DeviceDto("d1", "Test", "t1", null, null),
                ),
                deviceTypes = listOf(
                    DeviceTypeDto("t1", "Generic", null, 1, null),
                ),
                events = listOf(
                    BatteryEventDto("e1", "d1", "2024-01-01T00:00:00Z", null, null),
                ),
            )

            val result = useCase(jsonString)

            assertTrue(result is Result.Success)
            val importResult = (result as Result.Success).data
            assertEquals(1, importResult.devicesImported)
            assertEquals(1, importResult.deviceTypesImported)
            assertEquals(1, importResult.eventsImported)
        }

    // endregion

    // region Format versioning tests

    @Test
    fun `import rejects unknown formatVersion`() =
        runTest {
            val repository = TestImportRepository()
            val useCase = ImportDataUseCase(repository, testDispatcherProvider)

            val jsonString = makeValidJson(formatVersion = 999)

            val result = useCase(jsonString)

            assertTrue(result is Result.Error)
            val error = (result as Result.Error).error
            assertTrue(error.message.contains("999"))
            assertTrue(error.message.contains("Unsupported"))
        }

    @Test
    fun `import rejects wrong type field`() =
        runTest {
            val repository = TestImportRepository()
            val useCase = ImportDataUseCase(repository, testDispatcherProvider)

            val jsonString = makeValidJson(type = "something_else")

            val result = useCase(jsonString)

            assertTrue(result is Result.Error)
            val error = (result as Result.Error).error
            assertTrue(error.message.contains("something_else"))
        }

    @Test
    fun `import accepts formatVersion 1`() =
        runTest {
            val repository = TestImportRepository()
            val useCase = ImportDataUseCase(repository, testDispatcherProvider)

            val jsonString = makeValidJson(formatVersion = 1)

            val result = useCase(jsonString)

            assertTrue(result is Result.Success)
        }

    @Test
    fun `import ignores unknown keys in v1 data`() =
        runTest {
            val repository = TestImportRepository()
            val useCase = ImportDataUseCase(repository, testDispatcherProvider)

            // JSON with extra unknown fields at every level
            val jsonString =
                """
                {
                    "formatVersion": 1,
                    "type": "battery_butler_backup",
                    "futureField": "should be ignored",
                    "data": {
                        "devices": [
                            {
                                "id": "d1",
                                "name": "Test",
                                "typeId": "t1",
                                "batteryLastReplaced": "2024-01-01T00:00:00Z",
                                "location": null,
                                "newDeviceField": 42
                            }
                        ],
                        "deviceTypes": [],
                        "events": [],
                        "futureDataField": []
                    }
                }
                """.trimIndent()

            val result = useCase(jsonString)

            assertTrue(result is Result.Success)
            val importResult = (result as Result.Success).data
            assertEquals(1, importResult.devicesImported)
        }

    // endregion

    // region Error handling tests

    @Test
    fun `import invalid JSON returns parse error`() =
        runTest {
            val repository = TestImportRepository()
            val useCase = ImportDataUseCase(repository, testDispatcherProvider)

            val result = useCase("not valid json at all")

            assertTrue(result is Result.Error)
            assertTrue((result as Result.Error).error.message.contains("could not parse"))
        }

    @Test
    fun `import truncated JSON returns parse error`() =
        runTest {
            val repository = TestImportRepository()
            val useCase = ImportDataUseCase(repository, testDispatcherProvider)

            val result = useCase("""{"formatVersion": 1, "type": "battery_butler_backup", "data": {""")

            assertTrue(result is Result.Error)
        }

    @Test
    fun `import malformed Instant string skips record and reports error`() =
        runTest {
            val repository = TestImportRepository()
            val useCase = ImportDataUseCase(repository, testDispatcherProvider)

            val jsonString = makeValidJson(
                events = listOf(
                    BatteryEventDto("e1", "d1", "not-a-date", null, null),
                    BatteryEventDto("e2", "d1", "2024-06-15T12:00:00Z", null, null),
                ),
            )

            val result = useCase(jsonString)

            assertTrue(result is Result.Success)
            val importResult = (result as Result.Success).data
            assertEquals(1, importResult.eventsImported) // second event succeeded
            assertEquals(1, importResult.errors.size) // first event failed
            assertTrue(importResult.errors.first().contains("e1"))
        }

    @Test
    fun `import with duplicate IDs upserts`() =
        runTest {
            val repository = TestImportRepository()
            val useCase = ImportDataUseCase(repository, testDispatcherProvider)

            val jsonString = makeValidJson(
                devices = listOf(
                    DeviceDto("d1", "First Name", "t1", "2024-01-01T00:00:00Z", null),
                    DeviceDto("d1", "Second Name", "t1", "2024-06-01T00:00:00Z", null),
                ),
            )

            val result = useCase(jsonString)

            assertTrue(result is Result.Success)
            val importResult = (result as Result.Success).data
            assertEquals(2, importResult.devicesImported) // both succeed (upsert)
            // Last write wins in repository
            assertEquals("Second Name", repository.devices.last { it.id == "d1" }.name)
        }

    // endregion

    // region Idempotency tests

    @Test
    fun `importing same file twice produces same result`() =
        runTest {
            val repository = TestImportRepository()
            val useCase = ImportDataUseCase(repository, testDispatcherProvider)

            val jsonString = makeValidJson(
                deviceTypes = listOf(DeviceTypeDto("t1", "Type", "AA", 1, null)),
                devices = listOf(DeviceDto("d1", "Device", "t1", "2024-01-01T00:00:00Z", null)),
                events = listOf(BatteryEventDto("e1", "d1", "2024-01-01T00:00:00Z", null, null)),
            )

            val result1 = useCase(jsonString)
            val result2 = useCase(jsonString)

            assertTrue(result1 is Result.Success)
            assertTrue(result2 is Result.Success)
            val import1 = (result1 as Result.Success).data
            val import2 = (result2 as Result.Success).data
            assertEquals(import1.devicesImported, import2.devicesImported)
            assertEquals(import1.deviceTypesImported, import2.deviceTypesImported)
            assertEquals(import1.eventsImported, import2.eventsImported)
        }

    @Test
    fun `import does not delete existing records not in backup`() =
        runTest {
            val repository = TestImportRepository()
            // Pre-populate with data not in the backup
            repository.devices.add(
                Device(
                    id = "existing-device",
                    name = "Existing",
                    typeId = "t1",
                    batteryLastReplaced = Instant.DISTANT_PAST,
                    lastUpdated = Instant.DISTANT_PAST,
                ),
            )

            val useCase = ImportDataUseCase(repository, testDispatcherProvider)

            val jsonString = makeValidJson(
                devices = listOf(
                    DeviceDto("new-device", "New", "t1", "2024-01-01T00:00:00Z", null),
                ),
            )

            useCase(jsonString)

            // Both existing and new device should be present
            assertTrue(repository.devices.any { it.id == "existing-device" })
            assertTrue(repository.devices.any { it.id == "new-device" })
        }

    // endregion
}

// Test repository that mirrors the one in ExportDataUseCaseTest
@OptIn(ExperimentalTime::class)
private class TestImportRepository : DeviceRepository {
    val devices = mutableListOf<Device>()
    val deviceTypes = mutableListOf<DeviceType>()
    val events = mutableListOf<BatteryEvent>()

    override val syncStatus: StateFlow<SyncStatus> = MutableStateFlow(SyncStatus.Idle)

    override fun dismissSyncStatus() {}

    override suspend fun resync() {}

    override suspend fun clearAllLocalData() {}

    override fun getAllDevices(): Flow<List<Device>> = flowOf(devices.toList())

    override fun getDeviceById(id: String): Flow<Device?> = flowOf(devices.find { it.id == id })

    override suspend fun addDevice(device: Device): Result<Unit, DataError> {
        devices.removeAll { it.id == device.id }
        devices.add(device)
        return Result.Success(Unit)
    }

    override suspend fun updateDevice(device: Device): Result<Unit, DataError> {
        devices.removeAll { it.id == device.id }
        devices.add(device)
        return Result.Success(Unit)
    }

    override suspend fun deleteDevice(id: String): Result<Unit, DataError> {
        devices.removeAll { it.id == id }
        return Result.Success(Unit)
    }

    override fun getAllDeviceTypes(): Flow<List<DeviceType>> = flowOf(deviceTypes.toList())

    override fun getDeviceTypeById(id: String): Flow<DeviceType?> = flowOf(deviceTypes.find { it.id == id })

    override suspend fun addDeviceType(type: DeviceType): Result<Unit, DataError> {
        deviceTypes.removeAll { it.id == type.id }
        deviceTypes.add(type)
        return Result.Success(Unit)
    }

    override suspend fun updateDeviceType(type: DeviceType): Result<Unit, DataError> {
        deviceTypes.removeAll { it.id == type.id }
        deviceTypes.add(type)
        return Result.Success(Unit)
    }

    override suspend fun deleteDeviceType(id: String): Result<Unit, DataError> {
        deviceTypes.removeAll { it.id == id }
        return Result.Success(Unit)
    }

    override fun getEventsForDevice(deviceId: String): Flow<List<BatteryEvent>> = flowOf(events.filter { it.deviceId == deviceId })

    override fun getAllEvents(): Flow<List<BatteryEvent>> = flowOf(events.toList())

    override fun getEventById(id: String): Flow<BatteryEvent?> = flowOf(events.find { it.id == id })

    override suspend fun addEvent(event: BatteryEvent): Result<Unit, DataError> {
        events.removeAll { it.id == event.id }
        events.add(event)
        return Result.Success(Unit)
    }

    override suspend fun updateEvent(event: BatteryEvent): Result<Unit, DataError> {
        events.removeAll { it.id == event.id }
        events.add(event)
        return Result.Success(Unit)
    }

    override suspend fun deleteEvent(id: String): Result<Unit, DataError> {
        events.removeAll { it.id == id }
        return Result.Success(Unit)
    }
}
