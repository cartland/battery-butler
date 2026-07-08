package com.chriscartland.batterybutler.datalocal

import com.chriscartland.batterybutler.datalocal.room.DatabaseFactory
import com.chriscartland.batterybutler.datalocal.room.DatabaseOption
import com.chriscartland.batterybutler.datalocal.room.DynamicDatabaseProvider
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.DataModeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class RoomLocalDataSourceTest {
    private val tmpDir = File(System.getProperty("java.io.tmpdir"))

    @BeforeTest
    @AfterTest
    fun cleanDbFiles() {
        DatabaseOption.baseFileNames.values.forEach { File(tmpDir, it).delete() }
    }

    @Test
    fun `clearAll empties devices, device types, and events`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val providerScope = CoroutineScope(SupervisorJob() + dispatcher)
            val factory = DatabaseFactory()
            val provider = DynamicDatabaseProvider(
                factory = factory,
                dataModeRepository = FakeDataModeRepo(DataMode.None),
                scope = providerScope,
            )
            val localDataSource = RoomLocalDataSource(provider)
            advanceUntilIdle()

            localDataSource.addDeviceType(DeviceType(id = "t1", name = "Smoke Alarm"))
            localDataSource.addDevice(
                Device(
                    id = "d1",
                    name = "Kitchen Smoke",
                    typeId = "t1",
                    batteryLastReplaced = Instant.DISTANT_PAST,
                    lastUpdated = Instant.DISTANT_PAST,
                ),
            )
            localDataSource.addEvent(BatteryEvent(id = "e1", deviceId = "d1", date = Instant.DISTANT_PAST))

            assertTrue(localDataSource.getAllDeviceTypes().first().isNotEmpty())
            assertTrue(localDataSource.getAllDevices().first().isNotEmpty())
            assertTrue(localDataSource.getAllEvents().first().isNotEmpty())

            localDataSource.clearAll()

            assertTrue(localDataSource.getAllDeviceTypes().first().isEmpty())
            assertTrue(localDataSource.getAllDevices().first().isEmpty())
            assertTrue(localDataSource.getAllEvents().first().isEmpty())

            providerScope.cancel()
        }

    private class FakeDataModeRepo(
        initial: DataMode,
    ) : DataModeRepository {
        private val mode = MutableStateFlow(initial)
        override val dataMode: Flow<DataMode> = mode

        override suspend fun setDataMode(m: DataMode) {
            mode.value = m
        }
    }
}
