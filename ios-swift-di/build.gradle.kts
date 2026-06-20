import co.touchlab.skie.configuration.SuspendInterop

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
            binaryOption("bundleId", "com.chriscartland.batterybutler.shared")
            export(project(":domain"))
            export(project(":viewmodel"))
            export(project(":presentation-model"))
            export(project(":experimental:viewmodel"))
            export(project(":experimental:domain"))
            export(libs.androidx.lifecycle.viewmodel)
            // bb-ovm1 spike: export KMP-ObservableViewModel core so its ViewModel base +
            // KMPObservableViewModelCoreObjC cinterop are visible to the Swift SPM package.
            export(libs.kmp.observableviewmodel.core)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)

            implementation(project(":data"))
            implementation(project(":ai"))
            implementation(project(":usecase"))
            api(project(":viewmodel"))
            api(project(":presentation-model"))
            api(project(":experimental:viewmodel"))
            // bb-ovm1 spike: required as api so the framework can export() it (above).
            api(libs.kmp.observableviewmodel.core)
            implementation(project(":experimental:usecase"))
            implementation(project(":experimental:data-local"))

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

skie {
    features {
        group {
            // All ViewModel actions are fire-and-forget (viewModelScope.launch); no Swift code
            // awaits a Kotlin suspend function, so SuspendInterop is unused and disabled.
            //
            // IMPORTANT: FlowInterop must stay ENABLED. Beyond exposing Flows as AsyncSequence,
            // it provides the strong Swift typing for `StateFlow.value`. The KMP-ObservableViewModel
            // `xxxValue` accessors read `flow.value`; with FlowInterop off, `.value` degrades to
            // `Any?` and every accessor fails to compile (verified locally — dev-mode CI skips iOS).
            // Enum/Sealed/DefaultArgument interop also stay enabled (load-bearing for Swift switches).
            SuspendInterop.Enabled(false)
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.kotlin.inject.compiler)
    add("kspIosX64", libs.kotlin.inject.compiler)
    add("kspIosArm64", libs.kotlin.inject.compiler)
    add("kspIosSimulatorArm64", libs.kotlin.inject.compiler)
}
