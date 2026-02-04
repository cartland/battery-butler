package com.chriscartland.batterybutler.datalocal.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import java.io.File

actual class DataStoreFactory {
    actual fun createPreferencesDataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                File(System.getProperty("java.io.tmpdir"), PREFERENCES_FILE_NAME)
                    .absolutePath
                    .toPath()
            },
        )
}
