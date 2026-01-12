plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ksp)
}

kotlin {
    applyDefaultHierarchyTemplate()
    iosX64 {
        binaries.framework {
            baseName = "shared"
            export(project(":domain"))
            export(project(":viewmodel"))
            export(project(":presentation-models"))
        }
    }
    iosArm64 {
        binaries.framework {
            baseName = "shared"
            export(project(":domain"))
            export(project(":viewmodel"))
            export(project(":presentation-models"))
        }
    }
    iosSimulatorArm64 {
        binaries.framework {
            baseName = "shared"
            export(project(":domain"))
            export(project(":viewmodel"))
            export(project(":presentation-models"))
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            api(project(":domain"))
            implementation(project(":data"))
            api(project(":viewmodel"))
            api(project(":presentation-models"))
            implementation(project(":usecase"))
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
