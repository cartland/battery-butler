import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

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
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }

    androidTarget {
        compilations.configureEach {
            if (name == "debug") {
                defaultSourceSet.kotlin.srcDir("src/screenshotTest/kotlin")
            }
        }
    }
}

android {
    namespace = "com.chriscartland.batterybutler.presentationcore"
    compileSdk = libs.versions.android.compileSdk
        .get()
        .toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk
            .get()
            .toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
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
            println("  Kotlin: ${ss.java.srcDirs}") // AndroidSourceSet treats python/kotlin often as 'java' in old api or 'kotlin' in new.
            // But let's just dump java srcDirs which usually includes kotlin in AGP
        }
    }
}
