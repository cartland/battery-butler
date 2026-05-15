package com.chriscartland.batterybutler.datalocal.room

expect class DatabaseFactory {
    fun createDatabase(option: DatabaseOption = DatabaseOption.Offline): AppDatabase

    fun evict(option: DatabaseOption)

    fun databaseFileExists(fileName: String): Boolean

    fun copyDatabaseFile(
        sourceFileName: String,
        destFileName: String,
    )

    /**
     * Rename a database file (and its SQLite WAL/SHM sidecars, if present).
     * Used by the atomic-swap restore flow to back up the active DB file
     * before overwriting it with a legacy file.
     */
    fun renameDatabaseFile(
        sourceFileName: String,
        destFileName: String,
    )

    /**
     * Delete a database file (and its SQLite WAL/SHM sidecars, if present).
     * Used to remove a backup once the restore has succeeded.
     */
    fun deleteDatabaseFile(fileName: String)

    /**
     * Returns the SQLite `PRAGMA user_version` of the file, or -1 if the file
     * does not exist or cannot be read. Used diagnostically (logged via Kermit)
     * to identify legacy file format before opening with Room.
     */
    fun readUserVersion(fileName: String): Int
}
