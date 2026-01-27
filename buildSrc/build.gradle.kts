plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

gradlePlugin {
    plugins {
        create("architectureCheck") {
            id = "architecture.check"
            implementationClass = "architecture.ArchitecturePlugin"
        }
    }
}
