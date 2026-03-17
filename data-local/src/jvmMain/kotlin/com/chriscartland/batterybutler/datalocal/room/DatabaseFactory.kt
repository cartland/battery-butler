package com.chriscartland.batterybutler.datalocal.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.chriscartland.batterybutler.datalocal.room.AppDatabase
import com.chriscartland.batterybutler.datalocal.room.AppDatabaseConstructor
import com.chriscartland.batterybutler.datalocal.room.MIGRATION_3_4
import com.chriscartland.batterybutler.datalocal.room.MIGRATION_4_5
import java.io.File

actual class DatabaseFactory {
    private val defaultInstance: AppDatabase by lazy {
        createNewDatabase(DatabaseConstants.PRODUCTION_DATABASE_NAME)
    }

    actual fun createDatabase(name: String): AppDatabase =
        if (name == DatabaseConstants.PRODUCTION_DATABASE_NAME) {
            defaultInstance
        } else {
            createNewDatabase(name)
        }

    private fun createNewDatabase(name: String): AppDatabase {
        val dbFile = File(System.getProperty("java.io.tmpdir"), name)
        return Room
            .databaseBuilder<AppDatabase>(
                name = dbFile.absolutePath,
                factory = { AppDatabaseConstructor.initialize() },
            ).setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
            .build()
    }
}
