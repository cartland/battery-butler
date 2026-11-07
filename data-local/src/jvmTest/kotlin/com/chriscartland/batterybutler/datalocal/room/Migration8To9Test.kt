package com.chriscartland.batterybutler.datalocal.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.chriscartland.batterybutler.datalocal.room.entity.LastSnapshotIdEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Opens a real version 8 database through Room, which validates the migrated tables against the
 * version 9 schema and throws on any difference. On a phone, that throw is a crash on update.
 *
 * On the JVM because `:data`'s `androidInstrumentedTest` MigrationTest isn't compiled by any task.
 */
class Migration8To9Test {
    @Test
    fun `an updating phone keeps its rows and starts with no remembered snapshot ids`() =
        runTest {
            val dir = Files.createTempDirectory("migration").toFile()
            val path = File(dir, "test.db").absolutePath
            val connection = BundledSQLiteDriver().open(path)
            VERSION_8_SCHEMA.forEach { connection.execSQL(it) }
            connection.execSQL("INSERT INTO battery_events (id, deviceId, date) VALUES ('e1', 'd1', 1000)")
            connection.execSQL("PRAGMA user_version = 8")
            connection.close()

            val db = Room
                .databaseBuilder<AppDatabase>(name = path, factory = { AppDatabaseConstructor.initialize() })
                .setDriver(BundledSQLiteDriver())
                .addMigrations(MIGRATION_8_9)
                .build()
            try {
                val dao = db.deviceDao()
                assertEquals(listOf("e1"), dao.getAllEvents().first().map { it.id })
                assertEquals(emptyList(), dao.getLastSnapshotIds())

                val id = LastSnapshotIdEntity(LastSnapshotIdEntity.KIND_EVENT, "e1")
                dao.insertLastSnapshotIds(listOf(id))
                assertEquals(listOf(id), dao.getLastSnapshotIds())
            } finally {
                db.close()
                dir.deleteRecursively()
            }
        }

    private companion object {
        /** From `schemas/.../8.json`. A shipped schema never changes, so this copy can't go stale. */
        val VERSION_8_SCHEMA = listOf(
            "CREATE TABLE IF NOT EXISTS `devices` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `typeId` TEXT NOT NULL, `batteryLastReplaced` INTEGER NOT NULL, `lastUpdated` INTEGER NOT NULL, `location` TEXT, `imagePath` TEXT, `imageEtag` TEXT, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `device_types` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `defaultIcon` TEXT, `batteryType` TEXT NOT NULL, `batteryQuantity` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `battery_events` (`id` TEXT NOT NULL, `deviceId` TEXT NOT NULL, `date` INTEGER NOT NULL, `batteryType` TEXT, `notes` TEXT, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `device_image_cache` (`imageEtag` TEXT NOT NULL, `bytes` BLOB NOT NULL, `contentType` TEXT NOT NULL, PRIMARY KEY(`imageEtag`))",
            "CREATE TABLE IF NOT EXISTS `needs_battery_flags` (`deviceId` TEXT NOT NULL, `flaggedAt` INTEGER NOT NULL, PRIMARY KEY(`deviceId`))",
            "CREATE INDEX IF NOT EXISTS `index_devices_typeId` ON `devices` (`typeId`)",
            "CREATE INDEX IF NOT EXISTS `index_battery_events_deviceId` ON `battery_events` (`deviceId`)",
            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'e175caab3a252e4a58d469f465be26e2')",
        )
    }
}
