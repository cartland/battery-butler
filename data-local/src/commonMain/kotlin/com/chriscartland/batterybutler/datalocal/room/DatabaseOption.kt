package com.chriscartland.batterybutler.datalocal.room

import com.chriscartland.batterybutler.domain.model.NetworkMode

enum class DatabaseCategory {
    Offline,
    Mock,
    LocalServer,
    ProductionServer,
    DevServer,
    LabsStaging,
    LabsProd,
}

/**
 * Identifies a local database file. [category] mirrors the [NetworkMode] subtype; [fileName] also
 * incorporates the mode's `url` (when it carries one) so two different real backends sharing a
 * subtype but a different URL never resolve to the same physical SQLite file — see
 * [fromNetworkMode]. Equality is structural (data class), so callers can keep comparing
 * `DatabaseOption` instances directly (e.g. [DynamicDatabaseProvider]'s `targetOption != currentOption`).
 */
data class DatabaseOption(
    val category: DatabaseCategory,
    val fileName: String,
) {
    companion object {
        /** Pre-existing, 1:1-per-category file names. LOCKED — see DatabaseOptionTest. */
        val baseFileNames: Map<DatabaseCategory, String> = mapOf(
            DatabaseCategory.Offline to "battery-butler-offline.db",
            DatabaseCategory.Mock to "battery-butler-mock.db",
            DatabaseCategory.LocalServer to "battery-butler-local-server.db",
            DatabaseCategory.ProductionServer to "battery-butler-production-server.db",
            DatabaseCategory.DevServer to "battery-butler-dev-server.db",
            DatabaseCategory.LabsStaging to "battery-butler-labs-staging.db",
            DatabaseCategory.LabsProd to "battery-butler-labs-prod.db",
        )

        /** File names from android/27 and earlier releases. */
        val legacyFileNames: Map<DatabaseCategory, String> = mapOf(
            DatabaseCategory.Offline to "battery-butler.db",
            DatabaseCategory.Mock to "battery-butler-dev.db",
        )

        val OFFLINE = DatabaseOption(DatabaseCategory.Offline, baseFileNames.getValue(DatabaseCategory.Offline))

        /**
         * Derives the [DatabaseOption] for [mode]. A mode with no url (or a blank one) resolves to
         * the bare per-category file name, unchanged from before this class tracked urls. A mode
         * with a real url gets that file name suffixed with a hash of the normalized url, so two
         * different urls under the same category (e.g. two [NetworkMode.GrpcAws] pointed at
         * different physical servers, as `ServerUrlReceiver` allows) never collide on one file.
         */
        fun fromNetworkMode(mode: NetworkMode): DatabaseOption {
            val category = categoryFor(mode)
            val base = baseFileNames.getValue(category)
            val key = urlKeyFor(mode)
            val fileName = if (key == null) base else "${base.removeSuffix(".db")}-$key.db"
            return DatabaseOption(category, fileName)
        }

        private fun categoryFor(mode: NetworkMode): DatabaseCategory =
            when (mode) {
                NetworkMode.None -> DatabaseCategory.Offline
                NetworkMode.Mock -> DatabaseCategory.Mock
                is NetworkMode.GrpcLocal -> DatabaseCategory.LocalServer
                is NetworkMode.GrpcAws -> DatabaseCategory.ProductionServer
                is NetworkMode.GrpcDev -> DatabaseCategory.DevServer
                is NetworkMode.LabsStaging -> DatabaseCategory.LabsStaging
                is NetworkMode.LabsProd -> DatabaseCategory.LabsProd
            }

        /** The mode's url, or null if it doesn't carry one / it's blank — never a literal "null" string. */
        private fun rawUrlFor(mode: NetworkMode): String? =
            when (mode) {
                is NetworkMode.GrpcLocal -> mode.url
                is NetworkMode.GrpcAws -> mode.url
                is NetworkMode.GrpcDev -> mode.url
                is NetworkMode.LabsStaging -> mode.url
                is NetworkMode.LabsProd -> mode.url
                NetworkMode.Mock, NetworkMode.None -> null
            }

        private fun urlKeyFor(mode: NetworkMode): String? {
            val normalized = rawUrlFor(mode)
                ?.trim()
                ?.ifBlank { null }
                ?.lowercase()
                ?.trimEnd('/') ?: return null
            return fnv1aHex(normalized)
        }

        /**
         * 32-bit FNV-1a -> 8 lowercase hex chars. Deterministic and filesystem-safe without
         * needing a platform MessageDigest (this class is commonMain, no platform crypto access).
         */
        private fun fnv1aHex(input: String): String {
            var hash = -0x7ee3623b // 0x811C9DC5 as a 32-bit signed literal
            for (b in input.encodeToByteArray()) {
                hash = hash xor (b.toInt() and 0xFF)
                hash *= 0x01000193
            }
            return hash.toUInt().toString(16).padStart(8, '0')
        }
    }
}
