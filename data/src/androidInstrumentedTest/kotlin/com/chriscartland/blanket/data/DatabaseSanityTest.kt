package com.chriscartland.blanket.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chriscartland.blanket.data.room.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DatabaseSanityTest {
    @Test
    fun verifyDatabaseOpens() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            // Use an in-memory database for sanity checking the schema validity
            val db = Room
                .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .setDriver(BundledSQLiteDriver())
                .build()

            // Open the database to verify schema validity
            // This will throw if the entities don't match the schema requirements
            // or if there are other configuration issues.
            val dao = db.deviceDao()
            assertNotNull(dao)

            // Verify we can query
            try {
                dao.getDeviceTypeCount()
            } catch (e: Exception) {
                throw IOException("Database failed to open or query: ${e.message}", e)
            } finally {
                db.close()
            }
        }
}
