package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.DeviceImageError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.testcommon.FakeDeviceImageRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UploadDeviceImageUseCaseTest {
    @Test
    fun `delegates to the repository and returns its result`() =
        runTest {
            val repo = FakeDeviceImageRepository().apply { uploadResult = Result.Success("etag-1") }
            val useCase = UploadDeviceImageUseCase(repo)

            val result = useCase("dev1", byteArrayOf(1, 2, 3), "image/jpeg")

            assertIs<Result.Success<String>>(result)
            assertEquals("etag-1", result.data)
        }

    @Test
    fun `propagates a repository error`() =
        runTest {
            val repo = FakeDeviceImageRepository().apply { uploadResult = Result.Error(DeviceImageError.TooLarge()) }
            val useCase = UploadDeviceImageUseCase(repo)

            val result = useCase("dev1", byteArrayOf(), "image/jpeg")

            assertIs<Result.Error<DeviceImageError>>(result)
            assertIs<DeviceImageError.TooLarge>(result.error)
        }
}
