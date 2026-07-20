package com.chriscartland.batterybutler.datalocal.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSRecursiveLock
import platform.Foundation.NSUserDomainMask
import platform.Foundation.closeFile
import platform.Foundation.fileHandleForReadingAtPath
import platform.Foundation.readDataOfLength
import platform.Foundation.seekToFileOffset

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

    @OptIn(ExperimentalForeignApi::class)
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

    @OptIn(ExperimentalForeignApi::class)
    actual fun renameDatabaseFile(
        sourceFileName: String,
        destFileName: String,
    ) {
        val dir = fileDirectory()
        val fileManager = NSFileManager.defaultManager
        val source = "$dir/$sourceFileName"
        val dest = "$dir/$destFileName"
        if (fileManager.fileExistsAtPath(dest)) {
            fileManager.removeItemAtPath(dest, null)
        }
        fileManager.moveItemAtPath(source, toPath = dest, error = null)
        listOf("-wal", "-shm").forEach { suffix ->
            val src = "$dir/$sourceFileName$suffix"
            val dst = "$dir/$destFileName$suffix"
            if (fileManager.fileExistsAtPath(src)) {
                if (fileManager.fileExistsAtPath(dst)) {
                    fileManager.removeItemAtPath(dst, null)
                }
                fileManager.moveItemAtPath(src, toPath = dst, error = null)
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun deleteDatabaseFile(fileName: String) {
        val dir = fileDirectory()
        val fileManager = NSFileManager.defaultManager
        listOf("", "-wal", "-shm").forEach { suffix ->
            val path = "$dir/$fileName$suffix"
            if (fileManager.fileExistsAtPath(path)) {
                fileManager.removeItemAtPath(path, null)
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun readUserVersion(fileName: String): Int {
        val path = "${fileDirectory()}/$fileName"
        if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return -1
        val handle: NSFileHandle = NSFileHandle.fileHandleForReadingAtPath(path) ?: return -1
        return try {
            handle.seekToFileOffset(60UL)
            val data: NSData = handle.readDataOfLength(4UL)
            if (data.length < 4UL) return -1
            val bytes = ByteArray(4)
            bytes.usePinned { pinned ->
                platform.posix.memcpy(pinned.addressOf(0), data.bytes, 4UL)
            }
            // SQLite header is big-endian
            val b0 = bytes[0].toInt() and 0xff
            val b1 = bytes[1].toInt() and 0xff
            val b2 = bytes[2].toInt() and 0xff
            val b3 = bytes[3].toInt() and 0xff
            (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            -1
        } finally {
            handle.closeFile()
        }
    }

    private fun createNewDatabase(option: DatabaseOption): AppDatabase {
        promoteBareFileIfNeeded(option)
        val dbFile = "${fileDirectory()}/${option.fileName}"
        return Room
            .databaseBuilder<AppDatabase>(
                name = dbFile,
                factory = { AppDatabaseConstructor.initialize() },
            ).setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            .fallbackToDestructiveMigrationFrom(false, 0, 1, 2)
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
