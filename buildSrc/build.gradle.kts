plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "7.0.4"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint()
    }
}

gradlePlugin {
    plugins {
        create("architectureCheck") {
            id = "architecture.check"
            implementationClass = "architecture.ArchitecturePlugin"
        }
    }
}
