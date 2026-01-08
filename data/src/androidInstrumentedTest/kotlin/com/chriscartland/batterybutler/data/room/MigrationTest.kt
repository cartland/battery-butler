package com.chriscartland.batterybutler.data.room

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    @Throws(IOException::class)
    fun migrate4To5() {
        var db = helper.createDatabase(TEST_DB, 4)

        // Database has schema version 4. Insert some data using SQL queries.
        // You cannot use the DAO classes because they expect the latest schema.
        db.execSQL("INSERT INTO battery_events (id, deviceId, date) VALUES ('1', 'device1', 123456789)")

        // Prepare for the next version.
        db.close()

        // Re-open the database with version 5 and provide MIGRATION_4_5.
        db = helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)

        // MigrationTestHelper automatically verifies the schema changes.
        // But we can check if the new columns are here and data is preserved.
        
        // Query to check data presence and new columns
        val cursor = db.query("SELECT * FROM battery_events WHERE id = '1'")
        assert(cursor.moveToFirst()) // Ensure the row still exists
        
        val idIndex = cursor.getColumnIndex("id")
        val batteryTypeIndex = cursor.getColumnIndex("batteryType")
        val notesIndex = cursor.getColumnIndex("notes")
        
        val id = cursor.getString(idIndex)
        // Check new columns exist (index != -1) and are null (default)
        assert(batteryTypeIndex != -1)
        assert(notesIndex != -1)
        
        val batteryType = if (cursor.isNull(batteryTypeIndex)) null else cursor.getString(batteryTypeIndex)
        val notes = if (cursor.isNull(notesIndex)) null else cursor.getString(notesIndex)
        
        assert(id == "1")
        assert(batteryType == null)
        assert(notes == null)
    }
    
    @Test
    @Throws(IOException::class)
    fun migrate3To5() {
        // Create earliest version
        helper.createDatabase(TEST_DB, 3).close()

        // Open latest version
        helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_3_4, MIGRATION_4_5)
    }
}
