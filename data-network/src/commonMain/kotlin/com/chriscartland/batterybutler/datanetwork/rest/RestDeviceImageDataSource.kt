package com.chriscartland.batterybutler.datanetwork.rest

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import com.chriscartland.batterybutler.domain.model.DeviceImageError
import com.chriscartland.batterybutler.domain.model.DispatcherProvider
import com.chriscartland.batterybutler.domain.model.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * REST implementation of the Labs backend's per-device photo endpoints
 * (`PUT/GET/DELETE .../devices/{id}/image`), one instance per Labs environment URL -- the same
 * construction shape as [RestRemoteDataSource] (shared [httpClient], per-call bearer token).
 * See `docs/DEVICE_IMAGES.md` §4b.
 */
internal class RestDeviceImageDataSource(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val tokenProvider: suspend () -> String?,
    // Photo up/downloads can be large; run them on the injected IO dispatcher so they use the
    // elastic IO pool (not the CPU-bound Default pool that also runs image decode) and so tests can
    // pin them to a test dispatcher. Production injects the shared provider via
    // DelegatingDeviceImageDataSource; the default only applies to standalone/test construction.
    private val dispatcherProvider: DispatcherProvider = DefaultRestDispatcherProvider,
) {
    suspend fun upload(
        deviceId: String,
        bytes: ByteArray,
        contentType: String,
    ): Result<String, DeviceImageError> =
        withContext(dispatcherProvider.io) {
            try {
                val token = tokenProvider()
                val response = httpClient.put(imageUrl(deviceId)) {
                    token?.let { bearerAuth(it) }
                    contentType(ContentType.parse(contentType))
                    setBody(bytes)
                }
                when {
                    response.status.isSuccess() -> Result.Success(response.body<UploadImageResponseWire>().imageEtag)
                    response.status == HttpStatusCode.BadRequest -> Result.Error(DeviceImageError.InvalidImage())
                    response.status == HttpStatusCode.NotFound -> Result.Error(DeviceImageError.DeviceNotFound())
                    response.status == HttpStatusCode.PayloadTooLarge -> Result.Error(DeviceImageError.TooLarge())
                    else -> Result.Error(DeviceImageError.NetworkError(cause = "HTTP ${response.status.value}"))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(TAG, e) { "upload($deviceId) failed" }
                Result.Error(DeviceImageError.NetworkError(cause = e.message))
            }
        }

    suspend fun fetch(deviceId: String): DeviceImageBytes? =
        withContext(dispatcherProvider.io) {
            try {
                val token = tokenProvider()
                val response = httpClient.get(imageUrl(deviceId)) { token?.let { bearerAuth(it) } }
                if (!response.status.isSuccess()) {
                    null
                } else {
                    DeviceImageBytes(
                        bytes = response.body<ByteArray>(),
                        contentType = response.headers[HttpHeaders.ContentType] ?: "application/octet-stream",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(TAG, e) { "fetch($deviceId) failed" }
                null
            }
        }

    suspend fun delete(deviceId: String): Boolean =
        withContext(dispatcherProvider.io) {
            try {
                val token = tokenProvider()
                val response = httpClient.delete(imageUrl(deviceId)) { token?.let { bearerAuth(it) } }
                response.status.isSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(TAG, e) { "delete($deviceId) failed" }
                false
            }
        }

    private fun imageUrl(deviceId: String): String = "${baseUrl.trimEnd('/')}$DEVICES_PATH/$deviceId/image"

    private companion object {
        const val TAG = "BatteryButlerRestImage"
        const val DEVICES_PATH = "/v1/battery-butler/devices"
    }
}
