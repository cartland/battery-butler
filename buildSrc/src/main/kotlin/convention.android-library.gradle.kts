plugins {
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.multiplatform")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    androidLibrary {
        compileSdk = libs
            .findVersion("android.compileSdk")
            .get()
            .requiredVersion
            .toInt()
        minSdk = libs
            .findVersion("android.minSdk")
            .get()
            .requiredVersion
            .toInt()
    }
    jvmToolchain(21)
}
