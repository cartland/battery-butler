plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.screenshot)
}

android {
    namespace = "com.chriscartland.screenshottests"
    compileSdk = libs.versions.android.compileSdk
        .get()
        .toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk
            .get()
            .toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    // kotlinOptions block removed, moved to extension level or specific target level if needed.
    // In kotlin-android plugin (single target), it's kotlin { compilerOptions { ... } }
    
    buildFeatures {
        compose = true
    }
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(compose.ui)
    implementation(compose.components.uiToolingPreview)
    implementation(compose.material3)
    debugImplementation(compose.uiTooling)

    screenshotTestImplementation(compose.uiTooling)
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.compose.ui.test.junit4)
    screenshotTestImplementation(libs.compose.ui.test.manifest)

    implementation(project(":ui-core"))
    implementation(project(":ui-feature"))
    implementation(project(":domain"))
    implementation(project(":usecase"))
    implementation(libs.kotlinx.datetime)
}
