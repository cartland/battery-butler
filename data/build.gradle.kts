import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    jvm()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":domain"))
            implementation(project(":networking"))
            implementation(libs.androidx.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.kotlin.inject.runtime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.uuid)
        }
        androidMain.dependencies {
            implementation(libs.androidx.room.runtime)
            implementation(libs.generativeai)
        }
            dependencies {
                implementation(libs.junit)
                implementation(libs.androidx.testExt.junit)
                implementation(libs.androidx.runner)
                implementation(libs.androidx.core)
                implementation(libs.androidx.room.testing)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

android {
    namespace = "com.chriscartland.batterybutler.data"
    compileSdk = libs.versions.android.compileSdk
        .get()
        .toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk
            .get()
            .toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }
        val geminiKey = localProperties.getProperty("GEMINI_API_KEY") ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    testOptions {
        managedDevices {
            devices {
                maybeCreate<com.android.build.api.dsl.ManagedVirtualDevice>("pixel5api34").apply {
                    device = "Pixel 5"
                    apiLevel = 34
                    systemImageSource = "google"
                }
            }
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Room: Use platform-specific KSP. Room generates 'actual' implementation.
    // add("kspCommonMainMetadata", libs.androidx.room.compiler)
    add("kspCommonMainMetadata", libs.kotlin.inject.compiler)

    add("kspAndroid", libs.androidx.room.compiler)
    add("kspAndroid", libs.kotlin.inject.compiler)
    add("kspAndroid", libs.kotlin.inject.compiler)

    add("kspIosX64", libs.androidx.room.compiler)
    add("kspIosX64", libs.kotlin.inject.compiler)

    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.kotlin.inject.compiler)

    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.kotlin.inject.compiler)

    // Add JVM KSP for Desktop support
    add("kspJvm", libs.androidx.room.compiler)
    add("kspJvm", libs.kotlin.inject.compiler)
}

configurations.named("kspCommonMainMetadata") {
    exclude(group = "androidx.room", module = "room-compiler")
}
