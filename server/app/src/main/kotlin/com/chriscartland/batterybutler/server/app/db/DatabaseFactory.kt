package com.chriscartland.batterybutler.server.app.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest

object DatabaseFactory {
    fun init() {
        val config = if (System.getenv("SERVER_LABEL") == "AWS Cloud") {
            getAwsDbConfig()
        } else {
            getLocalDbConfig()
        }

        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        transaction {
            SchemaUtils.create(
                com.chriscartland.batterybutler.server.app.repository.Devices,
                com.chriscartland.batterybutler.server.app.repository.DeviceTypes,
                com.chriscartland.batterybutler.server.app.repository.BatteryEvents,
            )
        }
    }

    private fun getAwsDbConfig(): HikariConfig {
        println("Initializing AWS Database Configuration...")
        val secretName = System.getenv("DB_SECRET_NAME") ?: error("DB_SECRET_NAME env var not found")
        val region = Region.US_WEST_1

        val client = SecretsManagerClient
            .builder()
            .region(region)
            .build()

        val getSecretValueRequest = GetSecretValueRequest
            .builder()
            .secretId(secretName)
            .build()

        val secretValue = client.getSecretValue(getSecretValueRequest).secretString()
        val credentials = Json { ignoreUnknownKeys = true }.decodeFromString<DbCredentials>(secretValue)

        return HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://${credentials.host}:${credentials.port}/${credentials.dbname}"
            username = credentials.username
            password = credentials.password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
    }

    private fun getLocalDbConfig(): HikariConfig {
        println("Initializing Local Database Configuration (H2)...")
        // For now, fallback to H2 in-memory or throw.
        // Using H2 for local dev to prevent crash until local postgres is set up.
        return HikariConfig().apply {
            driverClassName = "org.h2.Driver"
            jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
            maximumPoolSize = 3
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
    }

    @Serializable
    private data class DbCredentials(
        val username: String,
        val password: String,
        val host: String,
        val port: Int,
        val dbname: String,
    )
}
