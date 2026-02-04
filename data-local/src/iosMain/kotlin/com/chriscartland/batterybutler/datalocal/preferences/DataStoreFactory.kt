package com.chriscartland.batterybutler.datalocal.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual class DataStoreFactory {
    actual fun createPreferencesDataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                "${documentDirectory()}/$PREFERENCES_FILE_NAME".toPath()
            },
        )

    @OptIn(ExperimentalForeignApi::class)
    private fun documentDirectory(): String {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        return requireNotNull(documentDirectory?.path)
    }
}
