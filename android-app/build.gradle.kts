plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.screenshot)
}

android {
    namespace = "com.chriscartland.batterybutler.android"
    compileSdk = libs.versions.android.compileSdk
        .get()
        .toInt()

    defaultConfig {
        applicationId = "com.chriscartland.batterybutler"
        minSdk = libs.versions.android.minSdk
            .get()
            .toInt()
        targetSdk = libs.versions.android.targetSdk
            .get()
            .toInt()
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
    }

    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

dependencies {
    implementation(project(":compose-app"))
    implementation(project(":ui-core"))
    implementation(project(":data"))
    implementation(project(":networking"))
    implementation(project(":viewmodel"))
    implementation(project(":usecase"))
    implementation(project(":ui-feature"))

    implementation(project(":domain"))

    // Room dependencies for DatabaseFactory visibility/transitive usage
    implementation(libs.androidx.room.runtime)
    implementation(libs.sqlite.bundled)

    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlin.inject.runtime)
    implementation(libs.generativeai)
    implementation(libs.kotlinx.datetime)
    implementation(libs.uuid)
    implementation(libs.kotlinx.serialization.json)

    implementation(compose.material3)
    implementation(compose.ui)
    implementation(compose.components.uiToolingPreview)

    debugImplementation(compose.uiTooling)
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(compose.uiTooling)
}
