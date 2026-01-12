plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}

allprojects {
    apply(plugin = "com.diffplug.spotless")
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {

        if (project != rootProject) {
            kotlin {
                target(fileTree(mapOf("dir" to "src", "include" to "**/*.kt")))
                targetExclude(
                    "**/build/**",
                    "**/iosApp/**",
                    "ios-app-*/**",
                    "**/.gradle/**",
                    ".bazel",
                )
                ktlint()
            }
        }
        kotlinGradle {
            target(
                fileTree(projectDir) {
                    include("*.gradle.kts", "**/build.gradle.kts", "buildSrc/**/*.kts")
                    exclude(".bazel", "**/.bazel")
                },
            )
            targetExclude(
                "**/build/**",
                "**/iosApp/**",
                "ios-app-*/**",
                "**/.gradle/**",
            )
            ktlint()
        }
    }
}

tasks.register<GenerateGraphTask>("generateMermaidGraph") {
    group = "documentation"
    description = "Generates Mermaid architecture graph from project dependencies"
}
