package com.chriscartland.batterybutler.data.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.chriscartland.batterybutler.data.room.AppDatabase
import com.chriscartland.batterybutler.data.room.MIGRATION_3_4
import com.chriscartland.batterybutler.data.room.MIGRATION_4_5
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual class DatabaseFactory {
    actual fun createDatabase(): AppDatabase {
        val dbFile = "${fileDirectory()}/blanket.db"
        return Room
            .databaseBuilder<AppDatabase>(
                name = dbFile,
            ).setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
            .build()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun fileDirectory(): String {
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
