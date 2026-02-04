import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
    }

    // Force downgrade of dependencies that require AGP 8.9.1+
    // Removed as we are migrating to Nav2 which is compatible with stable AGP.

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.generativeai)
            implementation(libs.kotlinx.coroutines.play.services)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.viewmodel.navigation3)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(project(":data"))
            implementation(project(":presentation-core"))
            implementation(project(":viewmodel"))
            implementation(project(":ai"))
            implementation(project(":usecase"))
            // implementation(project(":networking")) // Provided by :data
            implementation(project(":compose-resources"))

            implementation(libs.androidx.nav3.ui)
            implementation(libs.androidx.nav3.runtime)
            implementation(libs.kotlin.inject.runtime)
            implementation(libs.kotlinx.datetime)
            implementation(libs.uuid)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.datastore.preferences.core)
            implementation(project(":presentation-feature"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.androidx.testExt.junit)
                implementation(libs.androidx.espresso.core)
                implementation(libs.androidx.runner)
                implementation(libs.androidx.core)
                implementation(libs.compose.ui.test.junit4)
                implementation(libs.wire.grpc.client)
            }
        }
    }

    sourceSets.all {
        languageSettings.optIn("kotlin.time.ExperimentalTime")
    }
}

android {
    namespace = "com.chriscartland.batterybutler.composeapp"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "com.chriscartland.batterybutler"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        // versionCode is set from tag in CI (android/N -> versionCode = N)
        // Pass via Gradle property: -PVERSION_CODE=123
        versionCode = (findProperty("VERSION_CODE") as String?)?.toIntOrNull() ?: 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }
        val geminiKey = localProperties.getProperty("GEMINI_API_KEY") ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
    }

    // Signing configuration for release builds (CI environment or Local)
    signingConfigs {
        create("release") {
            // 1. Try to load from keystore.properties (Local)
            val keystoreProps = Properties()
            val keystorePropsFile = rootProject.file("keystore.properties")
            if (keystorePropsFile.exists()) {
                logger.lifecycle("Signing: Loading properties from ${keystorePropsFile.absolutePath}")
                keystoreProps.load(keystorePropsFile.inputStream())
            } else {
                logger.lifecycle("Signing: No keystore.properties found at ${keystorePropsFile.absolutePath}")
            }

            // 2. Resolve values (Priority: Gradle Property (-P) > keystore.properties)
            val path = findProperty("KEYSTORE_PATH") as? String
                ?: keystoreProps.getProperty("KEYSTORE_PATH")
                ?: "release.keystore" // Default local path

            if (path != null) {
                // Try to resolve relative to root project first (common case for 'release.keystore')
                // OR handle absolute paths. rootProject.file() handles both well usually.
                var keystoreFile = rootProject.file(path)
                if (!keystoreFile.exists()) {
                    // Fallback: Check relative to module if root resolution failed (though unlikely for this setup)
                    // or just trust the logged output.
                    // But for 'release.keystore' default string, rootProject.file is correct.
                    // In CI, path is absolute.
                }

                if (keystoreFile.exists()) {
                    logger.lifecycle("Signing: Configuring release signing with keystore at ${keystoreFile.absolutePath}")
                    storeFile = keystoreFile
                    storePassword = findProperty("KEYSTORE_PASSWORD") as? String
                        ?: keystoreProps.getProperty("KEYSTORE_PASSWORD")
                    keyAlias = findProperty("KEY_ALIAS") as? String
                        ?: keystoreProps.getProperty("KEY_ALIAS")
                    keyPassword = findProperty("KEY_PASSWORD") as? String
                        ?: keystoreProps.getProperty("KEY_PASSWORD")
                } else {
                    logger.lifecycle(
                        "Signing: Release keystore not found at path: ${keystoreFile.absolutePath}. Release builds may invoke installation prompts or fail signing.",
                    )
                }
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Use release signing config if available
            val releaseSigningConfig = signingConfigs.findByName("release")
            if (releaseSigningConfig?.storeFile != null) {
                logger.lifecycle("Signing: Applying release signing config to release build type")
                signingConfig = releaseSigningConfig
            } else { // Explicitly log failure/skip
                logger.lifecycle("Signing: Release signing config has no storeFile. Skipping assignment (will use debug or unsigned).")
            }
        }
    }

    lint {
        disable += "NullSafeMutableLiveData"
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

dependencies {
    debugImplementation(compose.uiTooling)
    debugImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.leakcanary.android)
}

compose.desktop {
    application {
        mainClass = "com.chriscartland.batterybutler.composeapp.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.chriscartland.batterybutler"
            packageVersion = "1.0.0"
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.kotlin.inject.compiler)
    add("kspAndroid", libs.kotlin.inject.compiler)
    add("kspIosX64", libs.kotlin.inject.compiler)
    add("kspIosArm64", libs.kotlin.inject.compiler)
    add("kspIosSimulatorArm64", libs.kotlin.inject.compiler)
    add("kspDesktop", libs.kotlin.inject.compiler)
}

tasks.register("verifyIosFrameworkLinkage") {
    group = "verification"
    description = "Verifies iOS Framework Linkage by forcing linker errors."

    dependsOn("linkDebugFrameworkIosSimulatorArm64")

    // Configure task to fail on linker issues (similar to script flags)
    // Note: Project properties like -Pkotlin.native.binary.partialLinkage=disable are passed via command line
    // or can be set programmatically if possible, but dependsOn just invokes the task.
    // To strictly enforce flags, we might need an Exec task or doFirst configuration.
    // However, for a simple alias, this is a start. The script used:
    // -Pkotlin.native.binary.partialLinkage=disable --info

    doFirst {
        println(
            "Note: For stricter verification, run with: ./gradlew verifyIosFrameworkLinkage -Pkotlin.native.binary.partialLinkage=disable",
        )
    }
}
