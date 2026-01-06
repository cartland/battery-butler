import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":domain"))
            api(libs.androidx.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.kotlin.inject.runtime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            implementation(libs.androidx.room.runtime)
        }
        // Room compiler via KSP is configured in dependencies block
    }
}

android {
    namespace = "com.chriscartland.blanket.data"
    compileSdk = libs.versions.android.compileSdk
        .get()
        .toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk
            .get()
            .toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Room: Use platform-specific KSP. Room generates 'actual' implementation.
    // add("kspCommonMainMetadata", libs.androidx.room.compiler)
    add("kspCommonMainMetadata", libs.kotlin.inject.compiler)
    
    add("kspAndroid", libs.androidx.room.compiler)
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
