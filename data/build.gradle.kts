import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    // alias(libs.plugins.room) // Removed
    alias(libs.plugins.ksp)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }

    jvm {
        compilerOptions {
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":domain"))
            api(project(":data-network"))
            api(project(":data-local"))

            // implementation(libs.androidx.room.runtime) // Moved to data-local
            // implementation(libs.sqlite.bundled) // Moved to data-local
            implementation(libs.kotlin.inject.runtime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.uuid)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            // implementation(libs.androidx.room.runtime) // Moved
            // implementation(libs.generativeai) // Moved to :ai
        }
        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.androidx.testExt.junit)
                implementation(libs.androidx.runner)
                implementation(libs.androidx.core)
                // implementation(libs.androidx.room.testing)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }

    sourceSets.all {
        languageSettings.optIn("kotlin.time.ExperimentalTime")
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

// Room block removed

ksp {
    // Room schema arg removed
}

dependencies {
    // Room KSP processors removed

    add("kspCommonMainMetadata", libs.kotlin.inject.compiler)

    add("kspAndroid", libs.kotlin.inject.compiler)
    add("kspAndroid", libs.kotlin.inject.compiler)

    add("kspIosX64", libs.kotlin.inject.compiler)

    add("kspIosArm64", libs.kotlin.inject.compiler)

    add("kspIosSimulatorArm64", libs.kotlin.inject.compiler)

    // Add JVM KSP for Desktop support
    add("kspJvm", libs.kotlin.inject.compiler)
}

configurations.named("kspCommonMainMetadata") {
    // exclude(group = "androidx.room", module = "room-compiler")
}
