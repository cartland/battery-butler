package com.chriscartland.batterybutler.testcommon

import com.chriscartland.batterybutler.domain.repository.LabsRefreshTokenPersistence

/** In-memory [LabsRefreshTokenPersistence] fake that records every write and clear. */
class FakeLabsRefreshTokenPersistence : LabsRefreshTokenPersistence {
    private val tokensByKey = mutableMapOf<String, String>()

    /** Every (environmentKey, refreshToken) passed to [save], in order. */
    val saves = mutableListOf<Pair<String, String>>()

    /** Every environmentKey passed to [clear], in order. */
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
