import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.file.SourceDirectorySet

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.wire)
}

kotlin {
    androidTarget()
    jvm("desktop")
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
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
            api(libs.wire.runtime)
            api(libs.wire.grpc.client)
        }
        androidMain.dependencies {
            api(libs.okhttp)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.chriscartland.batterybutler.networking"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
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
