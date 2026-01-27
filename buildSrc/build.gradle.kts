plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.android.tools.build:gradle:8.9.1")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.20")
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.2.20")
}

gradlePlugin {
    plugins {
        create("architectureCheck") {
            id = "architecture.check"
            implementationClass = "architecture.ArchitecturePlugin"
        }
    }
}
