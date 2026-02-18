plugins {
    id("convention.android-library")
    alias(libs.plugins.ksp)
}

kotlin {
    androidLibrary {
        namespace = "com.chriscartland.batterybutler.data"
        withDeviceTest {
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
            implementation(libs.multiplatform.settings)

            implementation(libs.kotlin.inject.runtime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.uuid)
            implementation(libs.androidx.datastore.preferences.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
        }
        val androidDeviceTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.androidx.testExt.junit)
                implementation(libs.androidx.runner)
                implementation(libs.androidx.core)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }

    sourceSets.all {
        languageSettings.optIn("kotlin.time.ExperimentalTime")
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.kotlin.inject.compiler)

    add("kspAndroid", libs.kotlin.inject.compiler)

    add("kspIosX64", libs.kotlin.inject.compiler)

    add("kspIosArm64", libs.kotlin.inject.compiler)

    add("kspIosSimulatorArm64", libs.kotlin.inject.compiler)

    // Add JVM KSP for Desktop support
    add("kspJvm", libs.kotlin.inject.compiler)
}
