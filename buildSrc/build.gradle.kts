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
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.10")
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.3.10")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        create("architectureCheck") {
            id = "architecture.check"
            implementationClass = "architecture.ArchitecturePlugin"
        }
        create("previewCoverageCheck") {
            id = "preview.coverage.check"
            implementationClass = "screenshotcoverage.PreviewCoveragePlugin"
        }
        create("themeLayerCheck") {
            id = "theme.layer.check"
            implementationClass = "themelayer.ThemeLayerPlugin"
        }
        create("testCoverageCheck") {
            id = "test.coverage.check"
            implementationClass = "testcoverage.TestCoveragePlugin"
        }
    }
}
