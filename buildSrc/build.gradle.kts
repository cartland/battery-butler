plugins {
    `kotlin-dsl`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.android.tools.build:gradle:8.9.1")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.0")
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.3.0")
}

gradlePlugin {
    plugins {
        create("architectureCheck") {
            id = "architecture.check"
            implementationClass = "architecture.ArchitecturePlugin"
        }
    }
}
