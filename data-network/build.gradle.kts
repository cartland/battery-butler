plugins {
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.wire)
}

kotlin {
    androidLibrary {
        namespace = "com.chriscartland.batterybutler.datanetwork"
        compileSdk = libs.versions.android.compileSdk
            .get()
            .toInt()
        minSdk = libs.versions.android.minSdk
            .get()
            .toInt()
    }
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

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
    val serverUrlProvider = providers.gradleProperty("PRODUCTION_SERVER_URL")
    val devServerUrlProvider = providers.gradleProperty("DEV_SERVER_URL")
    // Labs REST sync config (Workstream E). Blank when unset, so a Labs mode is selectable but
    // unconfigured until the owner provides these (ORG_GRADLE_PROJECT_LABS_* / local.properties).
    val labsFirebaseApiKeyProvider = providers.gradleProperty("LABS_FIREBASE_API_KEY")
    val labsStagingUrlProvider = providers.gradleProperty("LABS_STAGING_URL")
    val labsProdUrlProvider = providers.gradleProperty("LABS_PROD_URL")
    val generateBuildConfig = tasks.register("generateBuildConfig") {
        val buildConfigDir = layout.buildDirectory.dir("generated/buildConfig/commonMain")

        inputs.property("serverUrl", serverUrlProvider.orElse(""))
        inputs.property("devServerUrl", devServerUrlProvider.orElse(""))
        inputs.property("labsFirebaseApiKey", labsFirebaseApiKeyProvider.orElse(""))
        inputs.property("labsStagingUrl", labsStagingUrlProvider.orElse(""))
        inputs.property("labsProdUrl", labsProdUrlProvider.orElse(""))
        outputs.dir(buildConfigDir)

        doLast {
            val serverUrl = serverUrlProvider.orNull
            val devServerUrl = devServerUrlProvider.orNull
            val labsFirebaseApiKey = labsFirebaseApiKeyProvider.orElse("").get()
            val labsStagingUrl = labsStagingUrlProvider.orElse("").get()
            val labsProdUrl = labsProdUrlProvider.orElse("").get()

            val file = buildConfigDir.get().file("com/chriscartland/batterybutler/datanetwork/BuildConfig.kt").asFile
            file.parentFile.mkdirs()
            file.writeText(
                """
                package com.chriscartland.batterybutler.datanetwork

                object BuildConfig {
                    const val PRODUCTION_SERVER_URL = "$serverUrl"
                    const val DEV_SERVER_URL = "$devServerUrl"
                    const val LABS_FIREBASE_API_KEY = "$labsFirebaseApiKey"
                    const val LABS_STAGING_URL = "$labsStagingUrl"
                    const val LABS_PROD_URL = "$labsProdUrl"
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
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }

        androidMain.dependencies {
            implementation(libs.okhttp)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.googleid)
            implementation(libs.kotlinx.coroutines.play.services)
            implementation(libs.ktor.client.okhttp)
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }
        iosMain.dependencies {
            // ktor-client-core comes from commonMain; iOS only needs its engine.
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
    }

    sourceSets.all {
        languageSettings.optIn("kotlin.time.ExperimentalTime")
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
