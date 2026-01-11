plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.wire)
}

kotlin {
    androidTarget()
    jvm("desktop")

    // Task to run Bazel proto generation
    val generateProtos by tasks.registering(Exec::class) {
        commandLine(rootDir.resolve("scripts/generate-protos.sh"))
    }

    // Ensure generation runs before build
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        dependsOn(generateProtos)
    }
    tasks.withType<com.android.build.gradle.tasks.GenerateBuildConfig>().configureEach {
        dependsOn(generateProtos)
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Networking"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":domain"))
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.wire.runtime)
            implementation(libs.wire.grpc.client)
        }
        androidMain.dependencies {
            implementation(libs.okhttp)
        }
        val androidMain by getting {
            kotlin.srcDir("src/generated/java")
        }
        val desktopMain by getting {
            kotlin.srcDir("src/generated/java")
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.chriscartland.batterybutler.networking"
    compileSdk = libs.versions.android.compileSdk
        .get()
        .toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk
            .get()
            .toInt()
    }
}

wire {
    sourcePath {
        srcDir("../protos")
    }
    kotlin {
        rpcRole = "client"
        rpcCallStyle = "suspending"
    }
}
