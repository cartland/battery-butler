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
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.0")
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.4.0")
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
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
        create("namingConventionCheck") {
            id = "naming.convention.check"
            implementationClass = "namingconvention.NamingConventionPlugin"
        }
        create("importBoundaryCheck") {
            id = "import.boundary.check"
            implementationClass = "importboundary.ImportBoundaryPlugin"
        }
        create("dataStoreSingletonCheck") {
            id = "datastore.singleton.check"
            implementationClass = "datastoreguard.DataStoreSingletonPlugin"
        }
        create("hardcodedStringCheck") {
            id = "hardcoded.string.check"
            implementationClass = "stringresource.HardcodedStringPlugin"
        }
        create("previewTimeCheck") {
            id = "preview.time.check"
            implementationClass = "previewtime.PreviewTimePlugin"
        }
    }
}
