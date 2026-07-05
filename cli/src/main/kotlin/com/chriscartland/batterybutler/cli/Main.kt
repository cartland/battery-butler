package com.chriscartland.batterybutler.cli

import com.chriscartland.batterybutler.datanetwork.rest.SyncPushRequestWire
import com.chriscartland.batterybutler.datanetwork.rest.SyncPushResponseWire
import com.chriscartland.batterybutler.datanetwork.rest.SyncSnapshotWire
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlin.system.exitProcess

private const val SYNC_PATH = "/v1/battery-butler/sync"
private const val STAGING_URL = "https://cartland-labs-staging.web.app"
private const val PROD_URL = "https://cartland-labs.web.app"
private const val TOKEN_ENV_VAR = "BB_LABS_ID_TOKEN"

/**
 * Mirrors the JSON config in data-network's SyncHttpClient.kt (syncJson) so this CLI's parsing
 * and encoding matches exactly what the app sends/expects on the wire.
 */
private val syncJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
}

private sealed interface Command {
    data object Get : Command

    data class Push(
        val filePath: String,
    ) : Command
}

private data class ParsedArgs(
    val command: Command,
    val baseUrl: String,
    val token: String?,
    val showHelp: Boolean = false,
)

fun main(args: Array<String>) {
    val parsed =
        try {
            parseArgs(args)
        } catch (e: IllegalArgumentException) {
            System.err.println("Error: ${e.message}")
            printUsage()
            exitProcess(1)
        }

    if (parsed.showHelp) {
        printUsage()
        return
    }

    val token = parsed.token ?: System.getenv(TOKEN_ENV_VAR)
    if (token.isNullOrBlank()) {
        System.err.println("Error: no ID token provided. Pass --token <idToken> or set $TOKEN_ENV_VAR.")
        exitProcess(1)
    }

    val client =
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(syncJson) }
        }

    runBlocking {
        try {
            when (val command = parsed.command) {
                is Command.Get -> runGet(client, parsed.baseUrl, token)
                is Command.Push -> runPush(client, parsed.baseUrl, token, command.filePath)
            }
        } finally {
            client.close()
        }
    }
}

private suspend fun runGet(
    client: HttpClient,
    baseUrl: String,
    token: String,
) {
    val snapshot =
        try {
            val response = client.get("$baseUrl$SYNC_PATH") { bearerAuth(token) }
            if (!response.status.isSuccess()) {
                System.err.println("Error: sync request failed (HTTP ${response.status.value}).")
                exitProcess(1)
            }
            response.body<SyncSnapshotWire>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            System.err.println("Error: sync request failed: ${e.message}")
            exitProcess(1)
        }
    println(syncJson.encodeToString(SyncSnapshotWire.serializer(), snapshot))
}

private suspend fun runPush(
    client: HttpClient,
    baseUrl: String,
    token: String,
    filePath: String,
) {
    val file = File(filePath)
    if (!file.exists()) {
        System.err.println("Error: file not found: $filePath")
        exitProcess(1)
    }
    val request =
        try {
            syncJson.decodeFromString(SyncPushRequestWire.serializer(), file.readText())
        } catch (e: Exception) {
            System.err.println("Error: invalid push file ($filePath): ${e.message}")
            exitProcess(1)
        }
    val response =
        try {
            val httpResponse =
                client.post("$baseUrl$SYNC_PATH") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            if (!httpResponse.status.isSuccess()) {
                System.err.println("Error: sync push failed (HTTP ${httpResponse.status.value}).")
                exitProcess(1)
            }
            httpResponse.body<SyncPushResponseWire>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            System.err.println("Error: sync push failed: ${e.message}")
            exitProcess(1)
        }
    println(syncJson.encodeToString(SyncPushResponseWire.serializer(), response))
    if (!response.success) exitProcess(1)
}

private fun parseArgs(args: Array<String>): ParsedArgs {
    if (args.isEmpty() || args[0] == "--help" || args[0] == "-h") {
        return ParsedArgs(command = Command.Get, baseUrl = PROD_URL, token = null, showHelp = true)
    }

    val command: Command
    val rest: MutableList<String>
    when (val commandName = args[0]) {
        "get" -> {
            command = Command.Get
            rest = args.drop(1).toMutableList()
        }

        "push" -> {
            val tail = args.drop(1)
            val filePath =
                tail.firstOrNull { !it.startsWith("--") }
                    ?: throw IllegalArgumentException("push requires a file path")
            command = Command.Push(filePath)
            rest = tail.toMutableList().apply { remove(filePath) }
        }

        else -> {
            throw IllegalArgumentException("Unknown command: $commandName (expected 'get' or 'push')")
        }
    }

    var env: String? = null
    var url: String? = null
    var token: String? = null
    var i = 0
    while (i < rest.size) {
        when (rest[i]) {
            "--env" -> {
                env = rest.getOrNull(i + 1) ?: throw IllegalArgumentException("--env requires a value")
                i += 2
            }

            "--url" -> {
                url = rest.getOrNull(i + 1) ?: throw IllegalArgumentException("--url requires a value")
                i += 2
            }

            "--token" -> {
                token = rest.getOrNull(i + 1) ?: throw IllegalArgumentException("--token requires a value")
                i += 2
            }

            else -> {
                throw IllegalArgumentException("Unknown argument: ${rest[i]}")
            }
        }
    }

    val baseUrl =
        url ?: when (env) {
            null, "prod" -> PROD_URL
            "staging" -> STAGING_URL
            else -> throw IllegalArgumentException("Unknown --env '$env' (expected 'staging' or 'prod')")
        }

    return ParsedArgs(command = command, baseUrl = baseUrl, token = token)
}

private fun printUsage() {
    println(
        """
        Battery Butler Labs CLI — reads/writes synced data via the Labs REST backend.

        Usage:
          bb-labs-cli get  [--env staging|prod] [--url <base-url>] [--token <idToken>]
          bb-labs-cli push <file.json> [--env staging|prod] [--url <base-url>] [--token <idToken>]

        Options:
          --env <staging|prod>   Target environment (default: prod). Ignored if --url is set.
          --url <base-url>       Override the base URL directly.
          --token <idToken>      Labs Firebase ID token. If omitted, reads from $TOKEN_ENV_VAR.

        The token is a live credential — obtain it from the app's Settings → Advanced →
        "Copy Labs ID Token" (it expires after about an hour). Never share it.
        """.trimIndent(),
    )
}
