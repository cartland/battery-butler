rootProject.name = "BatteryButler"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":compose-resources")
include(":compose-app")
include(":server:domain")
include(":server:data")
include(":server:app")
include(":server:app")
include(":data-network")
include(":data-local")
include(":ios-swift-di")
include(":android-screenshot-tests")
include(":domain")
include(":usecase")
include(":data")
include(":presentation-core")
include(":presentation-feature")
include(":presentation-model")
include(":viewmodel")
