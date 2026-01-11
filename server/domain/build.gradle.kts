plugins {
    alias(libs.plugins.kotlinJvm)
}

group = "com.chriscartland.batterybutler.domain"
version = "1.0.0"

dependencies {
    implementation(project(":domain"))
    implementation(libs.kotlinx.coroutines.core)
}
