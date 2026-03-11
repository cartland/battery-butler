plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.screenshot)
}

android {
    namespace = "com.chriscartland.batterybutler.androidscreenshottests"
    compileSdk = libs.versions.android.compileSdk
        .get()
        .toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk
            .get()
            .toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    // kotlinOptions block removed, moved to extension level or specific target level if needed.
    // In kotlin-android plugin (single target), it's kotlin { compilerOptions { ... } }

    buildFeatures {
        compose = true
    }
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
}

dependencies {
    implementation(compose.ui)
    implementation(compose.components.uiToolingPreview)
    implementation(compose.components.resources)
    implementation(compose.material3)
    debugImplementation(compose.uiTooling)
    implementation(compose.materialIconsExtended)

    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.compose.ui.test.junit4)
    screenshotTestImplementation(libs.compose.ui.test.manifest)

    implementation(project(":presentation-core"))
    implementation(project(":presentation-feature"))
    implementation(project(":domain"))
    implementation(project(":compose-resources"))

    implementation(project(":presentation-model"))
    implementation(project(":experimental"))
    implementation(libs.kotlinx.datetime)
}

tasks.register<Exec>("generateScreenshotGallery") {
    group = "screenshot"
    description = "Generates a Markdown gallery of all reference screenshots."
    // Script is located at project_root/scripts/generate-android-screenshot-gallery.sh
    // build.gradle.kts is in android-screenshot-tests/
    // So distinct path is ../scripts/generate-android-screenshot-gallery.sh from projectDir?
    // Gradle executes in projectDir usually.
    workingDir = rootProject.projectDir
    commandLine("./scripts/generate-android-screenshot-gallery.sh")
}

tasks.register("cleanReferenceScreenshots") {
    group = "screenshot"
    description = "Cleans the reference screenshots directory."
    doLast {
        val referenceDir = file("src/screenshotTestDebug/reference")
        if (referenceDir.exists()) {
            referenceDir.deleteRecursively()
            println("Deleted reference screenshots: $referenceDir")
        }
    }
}

tasks.whenTaskAdded {
    if (name == "updateDebugScreenshotTest") {
        if (!project.hasProperty("retainedReferenceScreenshots")) {
            dependsOn("cleanReferenceScreenshots")
        }
        finalizedBy("generateScreenshotGallery")
    }
    if (name in listOf("updateDebugScreenshotTest", "validateDebugScreenshotTest")) {
        doFirst {
            val isSequentialScript = project.hasProperty("retainedReferenceScreenshots")
            val isForced = project.hasProperty("forceAllScreenshots")
            if (!isSequentialScript && !isForced) {
                error(
                    """

                    ===========================================================
                    BLOCKED: Running all screenshot tests in a single Gradle
                    invocation may cause OutOfMemoryError.
                    ===========================================================

                    Use the sequential script instead:
                      ./scripts/generate-android-screenshots.sh

                    To force a single-invocation run (e.g. on a machine with
                    enough memory), add the Gradle property:
                      -PforceAllScreenshots

                    Example:
                      ./gradlew :android-screenshot-tests:$name -PforceAllScreenshots

                    If the task hangs or is killed, the environment likely does
                    not have enough memory. Consider wrapping the command in a
                    timeout to detect this, then use the sequential script instead.
                    ===========================================================

                    """.trimIndent(),
                )
            }
        }
    }
}
