package com.chriscartland.batterybutler.datalocal.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSRecursiveLock
import platform.Foundation.NSUserDomainMask

actual class DatabaseFactory {
    private val lock = NSRecursiveLock()
    private val instances = mutableMapOf<DatabaseOption, AppDatabase>()

    actual fun createDatabase(option: DatabaseOption): AppDatabase {
        lock.lock()
        try {
            return instances.getOrPut(option) { createNewDatabase(option) }
        } finally {
            lock.unlock()
        }
    }

    actual fun evict(option: DatabaseOption) {
        lock.lock()
        try {
            instances.remove(option)
        } finally {
            lock.unlock()
        }
    }

    actual fun databaseFileExists(fileName: String): Boolean {
        val path = "${fileDirectory()}/$fileName"
        return NSFileManager.defaultManager.fileExistsAtPath(path)
    }

    actual fun copyDatabaseFile(
        sourceFileName: String,
        destFileName: String,
    ) {
        val dir = fileDirectory()
        val source = "$dir/$sourceFileName"
        val dest = "$dir/$destFileName"
        val fileManager = NSFileManager.defaultManager
        if (fileManager.fileExistsAtPath(dest)) {
            fileManager.removeItemAtPath(dest, null)
        }
        fileManager.copyItemAtPath(source, toPath = dest, error = null)
    }

    private fun createNewDatabase(option: DatabaseOption): AppDatabase {
        val dbFile = "${fileDirectory()}/${option.fileName}"
        return Room
            .databaseBuilder<AppDatabase>(
                name = dbFile,
                factory = { AppDatabaseConstructor.initialize() },
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
