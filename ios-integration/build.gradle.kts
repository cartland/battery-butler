plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ksp)
}

kotlin {
    iosX64 {
        binaries.framework {
            baseName = "shared"
            export(project(":domain"))
            export(project(":viewmodel"))
        }
    }
    iosArm64 {
        binaries.framework {
            baseName = "shared"
            export(project(":domain"))
            export(project(":viewmodel"))
        }
    }
    iosSimulatorArm64 {
        binaries.framework {
            baseName = "shared"
            export(project(":domain"))
            export(project(":viewmodel"))
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            api(project(":domain"))
            implementation(project(":data"))
            api(project(":viewmodel"))
            implementation(project(":usecase"))
            implementation(libs.kotlin.inject.runtime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.kotlin.inject.compiler)
    add("kspIosX64", libs.kotlin.inject.compiler)
    add("kspIosArm64", libs.kotlin.inject.compiler)
    add("kspIosSimulatorArm64", libs.kotlin.inject.compiler)
}
