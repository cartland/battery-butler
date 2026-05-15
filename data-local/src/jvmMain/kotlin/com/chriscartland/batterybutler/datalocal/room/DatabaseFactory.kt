package com.chriscartland.batterybutler.datalocal.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import java.io.RandomAccessFile

actual class DatabaseFactory {
    private val instances = mutableMapOf<DatabaseOption, AppDatabase>()
    private val baseDir = File(System.getProperty("java.io.tmpdir"))

    @Synchronized
    actual fun createDatabase(option: DatabaseOption): AppDatabase = instances.getOrPut(option) { createNewDatabase(option) }

    @Synchronized
    actual fun evict(option: DatabaseOption) {
        instances.remove(option)
    }

    actual fun databaseFileExists(fileName: String): Boolean = File(baseDir, fileName).exists()

    actual fun copyDatabaseFile(
        sourceFileName: String,
        destFileName: String,
    ) {
        val source = File(baseDir, sourceFileName)
        val dest = File(baseDir, destFileName)
        source.copyTo(dest, overwrite = true)
    }

    actual fun renameDatabaseFile(
        sourceFileName: String,
        destFileName: String,
    ) {
        val source = File(baseDir, sourceFileName)
        val dest = File(baseDir, destFileName)
        if (dest.exists()) dest.delete()
        source.renameTo(dest)
        listOf("-wal", "-shm").forEach { suffix ->
            val src = File(baseDir, sourceFileName + suffix)
            val dst = File(baseDir, destFileName + suffix)
            if (src.exists()) {
                if (dst.exists()) dst.delete()
                src.renameTo(dst)
            }
        }
    }

    actual fun deleteDatabaseFile(fileName: String) {
        listOf("", "-wal", "-shm").forEach { suffix ->
            val f = File(baseDir, fileName + suffix)
            if (f.exists()) f.delete()
        }
    }

    actual fun readUserVersion(fileName: String): Int {
        val file = File(baseDir, fileName)
        if (!file.exists()) return -1
        return try {
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(60)
                raf.readInt()
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            -1
        }
    }

    private fun createNewDatabase(option: DatabaseOption): AppDatabase {
        val dbFile = File(baseDir, option.fileName)
        return Room
            .databaseBuilder<AppDatabase>(
                name = dbFile.absolutePath,
                factory = { AppDatabaseConstructor.initialize() },
            ).setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
            .fallbackToDestructiveMigrationFrom(false, 0, 1, 2)
            .build()
    }
}
