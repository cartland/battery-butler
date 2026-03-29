package com.chriscartland.batterybutler.datalocal.room

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

actual class DatabaseFactory(
    private val context: Context,
) {
    private val instances = mutableMapOf<DatabaseOption, AppDatabase>()

    @Synchronized
    actual fun createDatabase(option: DatabaseOption): AppDatabase = instances.getOrPut(option) { createNewDatabase(option) }

    @Synchronized
    actual fun evict(option: DatabaseOption) {
        instances.remove(option)
    }

    actual fun databaseFileExists(fileName: String): Boolean = context.getDatabasePath(fileName).exists()

    actual fun copyDatabaseFile(
        sourceFileName: String,
        destFileName: String,
    ) {
        val source = context.getDatabasePath(sourceFileName)
        val dest = context.getDatabasePath(destFileName)
        source.copyTo(dest, overwrite = true)
    }

    private fun createNewDatabase(option: DatabaseOption): AppDatabase {
        val dbFile = context.getDatabasePath(option.fileName)
        return Room
            .databaseBuilder<AppDatabase>(
                context = context,
                name = dbFile.absolutePath,
                factory = { AppDatabaseConstructor.initialize() },
            ).setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
            .build()
    }
}
