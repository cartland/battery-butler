package com.chriscartland.batterybutler.datalocal.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

actual class DataStoreFactory(
    private val context: Context,
) {
    private val instance: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                context.filesDir
                    .resolve(PREFERENCES_FILE_NAME)
                    .absolutePath
                    .toPath()
            },
        )
    }

    actual fun createPreferencesDataStore(): DataStore<Preferences> = instance
}
