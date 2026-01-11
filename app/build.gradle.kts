plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.screenshot)
}

android {
    namespace = "com.chriscartland.batterybutler.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.chriscartland.batterybutler.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
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
    implementation(project(":ui-core"))
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(compose.components.uiToolingPreview)
    
    debugImplementation(compose.uiTooling)
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(compose.uiTooling)
}
