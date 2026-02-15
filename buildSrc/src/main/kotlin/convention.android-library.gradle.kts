import com.android.build.api.dsl.ManagedVirtualDevice
import org.gradle.api.JavaVersion
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // TODO: Use libs.plugins.androidKotlinMultiplatformLibrary
    // blocked by: buildSrc precompiled script plugins cannot access version catalog accessors easily.
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.multiplatform")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")


kotlin {
    androidLibrary {
        namespace = "com.chriscartland.batterybutler.placeholder" // Should be overridden by module?
        // Convention plugin cannot easily set namespace if it varies.
        // But the original convention plugin didn't set namespace in android {} block?
        // Wait, original file: android { compileSdk = ... }
        // It did NOT set namespace. Namespace was set in module build.gradle.kts.
        
        compileSdk = libs.findVersion("android.compileSdk").get().requiredVersion.toInt()
        minSdk = libs.findVersion("android.minSdk").get().requiredVersion.toInt()
        
        // withAndroidTest() // Disabled for now
    }
    
    // androidTarget block removed.
}

// android { ... } block removed because it's incompatible/unresolved.
// Modules utilizing this convention MUST set namespace in their build files.
// But wait, they used 'android { namespace = ... }'.
// Now they must use 'kotlin { androidLibrary { namespace = ... } }'.
// Does the convention plugin block prevent modules from configuring it further?
// No, Gradle allows multiple configuration blocks.

