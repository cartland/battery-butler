plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.ksp)
}

kotlin {
    androidLibrary {
        namespace = "com.chriscartland.batterybutler.shared"
        compileSdk = libs.versions.android.compileSdk
            .get()
            .toInt()
        minSdk = libs.versions.android.minSdk
            .get()
            .toInt()
        withHostTestBuilder {
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }
    }

    iosX64 {
        binaries.framework {
            baseName = "shared"
            export(project(":domain"))
            export(project(":viewmodel"))
        }
    }
    iosArm64 {
        binaries.framework {
            baseName = "shared"
            export(project(":domain"))
            export(project(":viewmodel"))
        }
    }
    iosSimulatorArm64 {
        binaries.framework {
            baseName = "shared"
            export(project(":domain"))
            export(project(":viewmodel"))
        }
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            api(project(":domain"))
            implementation(project(":data"))
            api(project(":viewmodel"))
            implementation(libs.kotlin.inject.runtime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.kotlin.inject.compiler)
    add("kspAndroid", libs.kotlin.inject.compiler)
    add("kspIosX64", libs.kotlin.inject.compiler)
    add("kspIosArm64", libs.kotlin.inject.compiler)
    add("kspIosSimulatorArm64", libs.kotlin.inject.compiler)
}
