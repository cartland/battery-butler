plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.ksp)
}

kotlin {
    androidLibrary {
        namespace = "com.chriscartland.batterybutler.viewmodel"
        compileSdk = libs.versions.android.compileSdk
            .get()
            .toInt()
        minSdk = libs.versions.android.minSdk
            .get()
            .toInt()
    }
    jvmToolchain(21)

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ViewModel"
            isStatic = true
        }
    }

    // JVM (Desktop)
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlinx.coroutines.core)
            api(libs.androidx.lifecycle.viewmodel)
            // bb-ovm1 spike: KMP-ObservableViewModel core (Option A — no NativeCoroutines yet).
            api(libs.kmp.observableviewmodel.core)
            implementation(libs.kotlin.inject.runtime)

            api(project(":presentation-model"))
            implementation(project(":domain"))
            implementation(projects.usecase)
            implementation(libs.kotlinx.datetime)
            implementation(libs.uuid)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(project(":test-common"))
        }
        val desktopTest by getting {
            dependencies {
                implementation(kotlin("reflect"))
                implementation(project(":experimental:viewmodel"))
            }
        }
    }

    sourceSets.all {
        languageSettings.optIn("kotlin.time.ExperimentalTime")
        // bb-ovm1 spike: KMP-ObservableViewModel's Apple stateIn actual uses cinterop.
        languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
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
