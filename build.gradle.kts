buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        // Fix Jib/Ktor plugin dependency conflict
        // See: https://github.com/GoogleContainerTools/jib/issues/4235
        // Jib 3.5.2 bytecode calls putArchiveEntry(TarArchiveEntry) which requires commons-compress 1.21
        // Downgrade to Jib 3.4.1 which is the last version properly compatible with 1.21
        classpath("com.google.cloud.tools:jib-gradle-plugin:3.4.1")
        classpath("org.apache.commons:commons-compress:1.21")
        classpath("commons-codec:commons-codec:1.16.1")
    }
    configurations.all {
        resolutionStrategy {
            // Force consistent versions for Jib compatibility
            // Jib 3.5.2 bytecode requires commons-compress 1.21 API (putArchiveEntry(TarArchiveEntry))
            // Ktor plugin pulls in newer jib which declares 1.26.0 but was compiled against 1.21
            force("com.google.cloud.tools:jib-gradle-plugin:3.4.1")
            force("org.apache.commons:commons-compress:1.21")
            force("commons-codec:commons-codec:1.16.1")
        }
    }
}

// Apply resolution strategy to all subprojects' buildscript configurations
subprojects {
    buildscript {
        configurations.all {
            resolutionStrategy {
                force("com.google.cloud.tools:jib-gradle-plugin:3.4.1")
                force("org.apache.commons:commons-compress:1.21")
                force("commons-codec:commons-codec:1.16.1")
            }
        }
    }
}

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    // alias(libs.plugins.androidApplication) apply false
    // alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    // alias(libs.plugins.kotlinJvm) apply false
    // alias(libs.plugins.kotlinAndroid) apply false
    // alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.spotless) apply false
    // alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    // alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.detekt) apply false
    id("architecture.check")
}

allprojects {
    apply(plugin = "com.diffplug.spotless")

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension> {
            jvmToolchain(21)
        }
    }
    pluginManager.withPlugin("org.jetbrains.kotlin.android") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension> {
            jvmToolchain(21)
        }
    }
    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension> {
            jvmToolchain(21)
        }
    }
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            if (project == rootProject) {
                target("src/**/*.kt", "buildSrc/src/**/*.kt")
            } else {
                target("src/**/*.kt")
            }
            ktlint()
        }
        kotlinGradle {
            target("*.gradle.kts")
            if (project == rootProject) {
                target("*.gradle.kts", "settings.gradle.kts", "buildSrc/**/*.kts")
                targetExclude("buildSrc/build/**/*.kts")
            }
            ktlint()
        }
    }

    apply(plugin = "io.gitlab.arturbosch.detekt")
    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(files("$rootDir/detekt.yml"))
        buildUponDefaultConfig = true
        // KMP source compatibility
        source.setFrom(files("src"))
    }

    dependencies {
        val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
        add("detektPlugins", libs.findLibrary("detekt-compose").get())
    }
}

tasks.register<modulegraph.GenerateGraphTask>("generateMermaidGraph") {
    group = "documentation"
    description = "Generates Mermaid architecture graph from project dependencies"
}

tasks.register<codeshareanalysis.CodeShareAnalysisTask>("analyzeCodeShare") {
    group = "documentation"
    description = "Generates a code sharing analysis report."
}
