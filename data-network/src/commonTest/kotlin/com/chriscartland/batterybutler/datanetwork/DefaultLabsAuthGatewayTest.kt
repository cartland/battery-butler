package com.chriscartland.batterybutler.datanetwork

import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.LabsFirebaseApiKey
import com.chriscartland.batterybutler.domain.model.SyncAuthReason
import com.chriscartland.batterybutler.domain.repository.DataModeRepository
import com.chriscartland.batterybutler.domain.repository.LabsRefreshTokenPersistence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DefaultLabsAuthGatewayTest {
    private class StaticDataModeRepository(
        mode: DataMode,
    ) : DataModeRepository {
        private val _dataMode = MutableStateFlow(mode)
        override val dataMode: Flow<DataMode> = _dataMode

        override suspend fun setDataMode(mode: DataMode) {
            _dataMode.value = mode
        }
    }

    private class RecordingRefreshTokenPersistence : LabsRefreshTokenPersistence {
        private val tokensByKey = mutableMapOf<String, String>()
        val saves = mutableListOf<Pair<String, String>>()
        val clears = mutableListOf<String>()

        override suspend fun get(environmentKey: String): String? = tokensByKey[environmentKey]

        override suspend fun save(
            environmentKey: String,
            refreshToken: String,
        ) {
            saves += environmentKey to refreshToken
            tokensByKey[environmentKey] = refreshToken
        }

        override suspend fun clear(environmentKey: String) {
            clears += environmentKey
            tokensByKey.remove(environmentKey)
        }
    }

    /**
     * The provider map is reached concurrently from the sync loop and sign-in. Pre-fix, an
     * unguarded `getOrPut` could construct two [com.chriscartland.batterybutler.datanetwork.rest
     * .FirebaseIdTokenProvider] instances for one environment and silently drop one — with it,
     * whichever in-memory session that instance held. Under real parallelism, all callers must
     * share ONE construction. (Against the unguarded code this fails flakily-but-reliably at
     * 200 parallel first-touches; the mutex makes it deterministic.)
     */
    @Test
    fun `concurrent first-touch constructs exactly one provider per environment`() =
        runTest {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val gateway = DefaultLabsAuthGateway(
                dataModeRepository = StaticDataModeRepository(DataMode.LabsStaging(url = "https://staging.example")),
                labsFirebaseApiKey = LabsFirebaseApiKey(staging = "staging-key", prod = "prod-key"),
                refreshTokenPersistence = RecordingRefreshTokenPersistence(),
                scope = scope,
            )

            withContext(Dispatchers.Default) {
                (1..200).map { launch { gateway.getLabsToken() } }.joinAll()
            }

            assertEquals(1, gateway.providerConstructionCount, "all concurrent callers must share one provider")
            assertEquals(setOf("staging-key"), gateway.providersByApiKey.keys)
            scope.cancel()
        }

    @Test
    fun `persistRotatedToken writes only when the token actually changed`() =
        runTest {
            val persistence = RecordingRefreshTokenPersistence()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val gateway = DefaultLabsAuthGateway(
                dataModeRepository = StaticDataModeRepository(DataMode.LabsStaging(url = "https://staging.example")),
                labsFirebaseApiKey = LabsFirebaseApiKey(staging = "staging-key", prod = "prod-key"),
                refreshTokenPersistence = persistence,
                scope = scope,
            )

            gateway.persistRotatedToken("staging-key", "refresh-1")
            assertEquals(listOf("staging-key" to "refresh-1"), persistence.saves, "first token must persist")

            gateway.persistRotatedToken("staging-key", "refresh-1")
            assertEquals(1, persistence.saves.size, "an unchanged token must not write to the shared DataStore")

            gateway.persistRotatedToken("staging-key", "refresh-2")
            assertEquals(
                listOf("staging-key" to "refresh-1", "staging-key" to "refresh-2"),
                persistence.saves,
                "a rotated token must persist",
            )
            scope.cancel()
        }

    /**
     * The single session-loss path: a terminal 401 report clears the persisted refresh token and
     * emits a [LabsSessionInvalidation] carrying the environment key + reason, so the auth
     * repository can flip the right environment's state. Local device data is not represented
     * here at all — structurally, the gateway cannot touch it.
     */
    @Test
    fun `reportSessionRejected clears the persisted token and emits an invalidation event`() =
        runTest {
            val persistence = RecordingRefreshTokenPersistence()
            persistence.save("staging-key", "refresh-1")
            persistence.saves.clear()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val gateway = DefaultLabsAuthGateway(
                dataModeRepository = StaticDataModeRepository(DataMode.LabsStaging(url = "https://staging.example")),
                labsFirebaseApiKey = LabsFirebaseApiKey(staging = "staging-key", prod = "prod-key"),
                refreshTokenPersistence = persistence,
                scope = scope,
            )
            val received = mutableListOf<LabsSessionInvalidation>()
            val collector = launch { gateway.sessionInvalidations.collect { received += it } }
            runCurrent() // the shared flow has no replay: subscribe before the emission

            gateway.reportSessionRejected(SyncAuthReason.TOKEN_EXPIRED)
            runCurrent()

            assertEquals(
                listOf(LabsSessionInvalidation(environmentKey = "staging-key", reason = SyncAuthReason.TOKEN_EXPIRED)),
                received,
            )
            assertEquals(listOf("staging-key"), persistence.clears, "the persisted refresh token must be cleared")
            assertTrue(persistence.saves.isEmpty())
            collector.cancel()
            scope.cancel()
        }

    /**
     * With no in-memory session and no persisted refresh token, the token result is a definitive
     * NoSession — and with a persisted token but an unconfigured/failing environment the caller
     * is never told NoSession spuriously (covered end-to-end by the provider + repository tests;
     * here we pin the no-persisted-token fast path fires no restore).
     */
    @Test
    fun `getLabsToken with nothing persisted reports NoSession`() =
        runTest {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val gateway = DefaultLabsAuthGateway(
                dataModeRepository = StaticDataModeRepository(DataMode.LabsStaging(url = "https://staging.example")),
                labsFirebaseApiKey = LabsFirebaseApiKey(staging = "staging-key", prod = "prod-key"),
                refreshTokenPersistence = RecordingRefreshTokenPersistence(),
                scope = scope,
            )

            assertIs<LabsTokenResult.NoSession>(gateway.getLabsToken())
            scope.cancel()
        }
}
