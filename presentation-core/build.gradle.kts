plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidLibrary {
        namespace = "com.chriscartland.batterybutler.presentationcore"
        compileSdk = libs.versions.android.compileSdk
            .get()
            .toInt()
        minSdk = libs.versions.android.minSdk
            .get()
            .toInt()
    }
    jvmToolchain(21)

    jvm()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(compose.materialIconsExtended)
            implementation(libs.kotlinx.datetime)

            implementation(project(":domain"))
            implementation(project(":presentation-model"))
            api(project(":compose-resources"))
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.exifinterface)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }

    targets.named("android") {
        compilations.configureEach {
            if (name == "debug") {
                defaultSourceSet.kotlin.srcDir("src/screenshotTest/kotlin")
            }
        }
    }
}

composeCompiler {
    stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("compose_compiler_config.conf"))
}

tasks.register("printCompilations") {
    doLast {
        println("--- Kotlin Android Target Compilations ---")
        kotlin.targets.matching { it.name == "androidTarget" || it.name == "android" }.forEach { target ->
            println("Target: ${target.name}")
            target.compilations.forEach { compilation ->
                println("  Compilation: ${compilation.name}")
                compilation.kotlinSourceSets.forEach { ss ->
                    println("    SourceSet: ${ss.name} (${ss.kotlin.srcDirs})")
                }
            }
        }
        println("\n--- Android SourceSets ---")
        val android = project.extensions.getByType(com.android.build.gradle.LibraryExtension::class.java)
        android.sourceSets.forEach { ss ->
            println("Android SourceSet: ${ss.name}")
            println("  Java: ${ss.java.srcDirs}")
            println("  Kotlin: ${ss.java.srcDirs}")
        }
    }
}
