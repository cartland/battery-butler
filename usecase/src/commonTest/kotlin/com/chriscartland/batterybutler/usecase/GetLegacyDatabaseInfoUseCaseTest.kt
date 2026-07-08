package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.LegacyDatabaseInfo
import com.chriscartland.batterybutler.testcommon.FakeLegacyDatabaseRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GetLegacyDatabaseInfoUseCaseTest {
    @Test
    fun `returns null for mode without legacy file`() {
        val repo = FakeLegacyDatabaseRepository()
        val useCase = GetLegacyDatabaseInfoUseCase(repo)

        val result = useCase(DataMode.GrpcLocal("http://localhost:50051"))

        assertNull(result)
    }

    @Test
    fun `returns legacy info when repository has data`() {
        val repo = FakeLegacyDatabaseRepository()
        repo.legacyInfoByMode[DataMode.None] = LegacyDatabaseInfo(
            legacyFileName = "battery-butler.db",
            exists = true,
        )
        val useCase = GetLegacyDatabaseInfoUseCase(repo)

        val result = useCase(DataMode.None)

        assertNotNull(result)
        assertEquals("battery-butler.db", result.legacyFileName)
        assertTrue(result.exists)
    }

    @Test
    fun `returns info with exists false when legacy file missing`() {
        val repo = FakeLegacyDatabaseRepository()
        repo.legacyInfoByMode[DataMode.Mock] = LegacyDatabaseInfo(
            legacyFileName = "battery-butler-dev.db",
            exists = false,
        )
        val useCase = GetLegacyDatabaseInfoUseCase(repo)

        val result = useCase(DataMode.Mock)

        assertNotNull(result)
        assertEquals("battery-butler-dev.db", result.legacyFileName)
        assertFalse(result.exists)
    }
}
