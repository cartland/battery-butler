plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.skie)
}

kotlin {
    applyDefaultHierarchyTemplate()
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            export(project(":domain"))
            export(project(":viewmodel"))
            export(project(":presentation-model"))
            export(libs.androidx.lifecycle.viewmodel)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)

            implementation(project(":data"))
            api(project(":viewmodel"))
            api(project(":presentation-model"))

            implementation(libs.kotlin.inject.runtime)
        }

        // Shared source code for iOS targets (to access KSP generated code)
        val iosShared = "src/iosShared/kotlin"
        val iosArm64Main by getting {
            kotlin.srcDir(iosShared)
        }
        val iosSimulatorArm64Main by getting {
            kotlin.srcDir(iosShared)
        }
        val iosX64Main by getting {
            kotlin.srcDir(iosShared)
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.kotlin.inject.compiler)
    add("kspIosX64", libs.kotlin.inject.compiler)
    add("kspIosArm64", libs.kotlin.inject.compiler)
    add("kspIosSimulatorArm64", libs.kotlin.inject.compiler)
}
