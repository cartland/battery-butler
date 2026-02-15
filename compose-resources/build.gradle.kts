import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    // TODO: Migrate to libs.plugins.androidKotlinMultiplatformLibrary (com.android.kotlin.multiplatform.library)
    // blocked by: New plugin does not generate AAR by default, causing missing assets/resources in consumer apps.
    // Upgrade when consumer is KMP or plugin supports AAR generation.
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
    }

    jvm()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }
    }
}



compose.resources {
    publicResClass = true
    packageOfResClass = "com.chriscartland.batterybutler.composeresources.generated.resources"
}

android {
    namespace = "com.chriscartland.batterybutler.composeresources"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
