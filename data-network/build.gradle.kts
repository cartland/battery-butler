import java.util.Properties

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.wire)
}

kotlin {
    androidTarget()
    jvm("desktop")

    // Wire Plugin generates code automatically. No custom task needed for Wire.
    // Bazel is used for Swift generation optimization.

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Networking"
        }
    }

    // Generate BuildConfig.kt for commonMain
    val generateBuildConfig = tasks.register("generateBuildConfig") {
        val buildConfigDir = layout.buildDirectory.dir("generated/buildConfig/commonMain")
        val localPropertiesFile = rootProject.file("local.properties")

        inputs.file(localPropertiesFile)
        outputs.dir(buildConfigDir)

        doLast {
            val properties = Properties()
            if (localPropertiesFile.exists()) {
                properties.load(localPropertiesFile.inputStream())
            }
            // Default URL if not present
            val defaultUrl = "http://battery-butler-nlb-847feaa773351518.elb.us-west-1.amazonaws.com:80"
            val serverUrl = properties.getProperty("PRODUCTION_SERVER_URL") ?: defaultUrl

            val file = buildConfigDir.get().file("com/chriscartland/batterybutler/datanetwork/BuildConfig.kt").asFile
            file.parentFile.mkdirs()
            file.writeText(
                """
                package com.chriscartland.batterybutler.datanetwork

                object BuildConfig {
                    const val PRODUCTION_SERVER_URL = "$serverUrl"
                }
                """.trimIndent(),
            )
        }
    }

    sourceSets {
        commonMain.configure {
            kotlin.srcDir(generateBuildConfig.map { it.outputs.files })
        }
        commonMain.dependencies {
            implementation(project(":domain"))
            implementation(project(":fixtures"))
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.wire.runtime)
            api(libs.wire.grpc.client)
            implementation(libs.kotlin.inject.runtime)
        }

        androidMain.dependencies {
            implementation(libs.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }

    sourceSets.all {
        languageSettings.optIn("kotlin.time.ExperimentalTime")
    }
}

android {
    namespace = "com.chriscartland.batterybutler.datanetwork"
    compileSdk = libs.versions.android.compileSdk
        .get()
        .toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk
            .get()
            .toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

wire {
    sourcePath {
        srcDir(file("../protos"))
    }
    kotlin {
        rpcRole = "client"
        rpcCallStyle = "suspending"
    }
}
