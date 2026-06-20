import co.touchlab.skie.configuration.SuspendInterop

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.skie)
}

kotlin {
    androidTarget()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "ExperimentalShared"
            isStatic = true
            binaryOption("bundleId", "com.chriscartland.batterybutler.experimental.shared")
            export(project(":experimental:viewmodel"))
            export(project(":experimental:domain"))
            export(libs.androidx.lifecycle.viewmodel)
        }
    }

    jvm("desktop")

    sourceSets {
        // Shared source code for iOS targets (to access KSP generated code)
        val iosShared = "src/iosShared/kotlin"
        val iosArm64Main by getting {
            kotlin.srcDir(iosShared)
        }
        val iosSimulatorArm64Main by getting {
            kotlin.srcDir(iosShared)
        }
        val iosX64Main by getting {
            kotlin.srcDir(iosShared)
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(project(":experimental:presentation-core"))
            api(project(":experimental:viewmodel"))
            api(project(":experimental:domain"))
            implementation(project(":experimental:usecase"))
            implementation(project(":experimental:data"))
            implementation(project(":experimental:data-local"))
            implementation(project(":data-local"))
            implementation(project(":domain"))
            implementation(libs.androidx.datastore.preferences.core)
            implementation(project(":presentation-core"))
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.viewmodel.navigation3)
            // bb-ovm1: declare runtimeCompose explicitly. It was pulled in transitively before;
            // adding kmp-observableviewmodel-core to :experimental:viewmodel shifted the
            // androidx.lifecycle graph and evicted it (collectAsStateWithLifecycle). The main
            // :compose-app already declares this directly.
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.nav3.ui)
            implementation(libs.androidx.nav3.runtime)
            implementation(libs.kotlin.inject.runtime)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}

skie {
    features {
        group {
            // No Swift code awaits a Kotlin suspend function, so SuspendInterop is unused.
            // FlowInterop stays enabled to keep `StateFlow.value` strongly typed in Swift for
            // the KMP-ObservableViewModel `.value` accessors (see ios-swift-di/build.gradle.kts).
            SuspendInterop.Enabled(false)
        }
    }
}

android {
    namespace = "com.chriscartland.batterybutler.experimental.composeapp"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()
    defaultConfig {
        applicationId = "com.chriscartland.batterybutler.experimental"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "0.1.0"
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

composeCompiler {
    stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("compose_compiler_config.conf"))
}
