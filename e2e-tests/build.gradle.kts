plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.wire)
}

dependencies {
    implementation(libs.wire.runtime)
    implementation(libs.wire.grpc.client)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.kotlinx.coroutines.test)
}

wire {
    sourcePath {
        srcDir(file("../protos"))
    }
    kotlin {
        rpcRole = "client"
        rpcCallStyle = "suspending"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.test {
    // Only run when explicitly invoked (e.g., ./gradlew :e2e-tests:test -De2e.server.url=...)
    // Prevents ./gradlew test from failing in CI where no server is available.
    onlyIf { System.getProperty("e2e.server.url") != null || gradle.startParameter.taskNames.any { it.contains("e2e-tests") } }

    systemProperty(
        "e2e.server.url",
        System.getProperty("e2e.server.url") ?: "http://localhost:50051",
    )
    systemProperty(
        "e2e.auth.token",
        System.getProperty("e2e.auth.token") ?: "",
    )
}
