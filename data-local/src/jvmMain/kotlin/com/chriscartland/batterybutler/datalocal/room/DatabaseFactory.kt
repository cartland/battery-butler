package com.chriscartland.batterybutler.datalocal.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

actual class DatabaseFactory {
    private val instances = mutableMapOf<DatabaseOption, AppDatabase>()

    @Synchronized
    actual fun createDatabase(option: DatabaseOption): AppDatabase = instances.getOrPut(option) { createNewDatabase(option) }

    @Synchronized
    actual fun evict(option: DatabaseOption) {
        instances.remove(option)
    }

    actual fun databaseFileExists(fileName: String): Boolean = File(System.getProperty("java.io.tmpdir"), fileName).exists()

    actual fun copyDatabaseFile(
        sourceFileName: String,
        destFileName: String,
    ) {
        val source = File(System.getProperty("java.io.tmpdir"), sourceFileName)
        val dest = File(System.getProperty("java.io.tmpdir"), destFileName)
        source.copyTo(dest, overwrite = true)
    }

    private fun createNewDatabase(option: DatabaseOption): AppDatabase {
        val dbFile = File(System.getProperty("java.io.tmpdir"), option.fileName)
        return Room
            .databaseBuilder<AppDatabase>(
                name = dbFile.absolutePath,
                factory = { AppDatabaseConstructor.initialize() },
            ).setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
            .build()
    }
}
