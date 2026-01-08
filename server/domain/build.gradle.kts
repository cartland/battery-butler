plugins {
    alias(libs.plugins.kotlinJvm)
}

group = "com.chriscartland.batterybutler.domain"
version = "1.0.0"

dependencies {
    api(project(":domain"))
    api(libs.kotlinx.coroutines.core)
}
