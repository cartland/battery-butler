package com.chriscartland.blanket.data.room

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val testDb = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migrate3To4() {
        var db = helper.createDatabase(testDb, 3)

        // Insert some data using SQL if possible, or verify schema.
        // For example, insert a device without 'location' column (it shouldn't exist).
        // db.execSQL("INSERT INTO devices ...") // Skipping data insert for now to focus on schema
        db.close()

        // Re-open the database with version 4 and provide MIGRATION_3_4
        db = helper.runMigrationsAndValidate(testDb, 4, true, MIGRATION_3_4)

        // Verify 'location' column exists or data integrity
        // The validate step above ensures the schema matches 4.json
    }
}
