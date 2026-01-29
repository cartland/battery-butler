package com.chriscartland.batterybutler.datalocal.room

/**
 * Constants for database configuration.
 */
data object DatabaseConstants {
    /** Production database name used for real data. */
    const val PRODUCTION_DATABASE_NAME = "battery-butler.db"

    /** Development database name used for mock/test data. */
    const val DEVELOPMENT_DATABASE_NAME = "battery-butler-dev.db"
}
