package com.chriscartland.batterybutler.testcommon

import com.chriscartland.batterybutler.datalocal.auth.LabsSessionStorage
import com.chriscartland.batterybutler.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [LabsSessionStorage] fake: one believed-signed-in user slot per environment key. */
class FakeLabsSessionStorage : LabsSessionStorage {
    private val usersByKey = mutableMapOf<String, MutableStateFlow<User?>>()

    var saveCount = 0
    var clearCount = 0

    private fun flowFor(environmentKey: String): MutableStateFlow<User?> = usersByKey.getOrPut(environmentKey) { MutableStateFlow(null) }

    override fun observeUser(environmentKey: String): Flow<User?> = flowFor(environmentKey)

    override suspend fun saveUser(
        environmentKey: String,
        user: User,
    ) {
        saveCount++
        flowFor(environmentKey).value = user
    }

    override suspend fun clearUser(environmentKey: String) {
        clearCount++
        flowFor(environmentKey).value = null
    }
}
