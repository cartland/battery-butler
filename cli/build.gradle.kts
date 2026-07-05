plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

kotlin {
    jvmToolchain(21)
}

group = "com.chriscartland.batterybutler.cli"
version = "1.0.0"

application {
    mainClass.set("com.chriscartland.batterybutler.cli.MainKt")
}

dependencies {
    // Reuses the Labs REST wire DTOs (SyncSnapshotWire, SyncPushRequestWire,
    // SyncPushResponseWire) so the CLI can't drift from the app's JSON contract.
    implementation(project(":data-network"))

    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    testImplementation(libs.kotlin.testJunit)
}
