package com.chriscartland.batterybutler.datalocal.room

/**
 * One-time migration for the url-aware [DatabaseOption] naming: if [option] resolved to a
 * url-suffixed file name and that file doesn't exist yet, but the category's pre-existing bare
 * file does, rename the bare file to the suffixed name so an existing install's data is inherited
 * rather than appearing empty. A no-op once a category's bare file has already been promoted (or
 * never existed), and a no-op for modes with no url (nothing to suffix).
 *
 * Must run before the caller opens Room on [option.fileName] — see each platform's
 * `DatabaseFactory.createNewDatabase`.
 */
internal fun DatabaseFactory.promoteBareFileIfNeeded(option: DatabaseOption) {
    val bareFileName = DatabaseOption.baseFileNames.getValue(option.category)
    if (option.fileName == bareFileName) return
    if (databaseFileExists(option.fileName)) return
    if (!databaseFileExists(bareFileName)) return
    renameDatabaseFile(bareFileName, option.fileName)
}
