buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        // Fix Jib/Ktor plugin dependency conflict
        // See: https://github.com/GoogleContainerTools/jib/issues/4235
        // Load Jib plugin through buildscript to control its classpath
        classpath("com.google.cloud.tools:jib-gradle-plugin:3.5.2")
        classpath("org.apache.commons:commons-compress:1.27.1")
        classpath("commons-codec:commons-codec:1.17.0")
    }
    configurations.all {
        resolutionStrategy {
            // Force consistent versions for Jib compatibility
            // 1.27.1 has the fix for the putArchiveEntry method signature
            force("org.apache.commons:commons-compress:1.27.1")
            force("commons-codec:commons-codec:1.17.0")
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
