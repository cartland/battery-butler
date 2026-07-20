package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import com.chriscartland.batterybutler.testcommon.FakeDeviceImageRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetCachedDeviceImageUseCaseTest {
    @Test
    fun `emits null when the etag isn't cached`() =
        runTest {
            val repo = FakeDeviceImageRepository()
            val useCase = GetCachedDeviceImageUseCase(repo)

            assertNull(useCase("missing-etag").first())
        }

    @Test
    fun `emits the cached bytes for a known etag`() =
        runTest {
            val repo = FakeDeviceImageRepository()
            val bytes = DeviceImageBytes(byteArrayOf(1, 2, 3), "image/jpeg")
            repo.setCachedImage("etag-1", bytes)
            val useCase = GetCachedDeviceImageUseCase(repo)

            val result = useCase("etag-1").first()

            assertEquals(bytes.contentType, result?.contentType)
            assertEquals(listOf<Byte>(1, 2, 3), result?.bytes?.toList())
        }
}
