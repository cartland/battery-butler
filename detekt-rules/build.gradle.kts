plugins {
    alias(libs.plugins.kotlinJvm)
}

dependencies {
    compileOnly("io.gitlab.arturbosch.detekt:detekt-api:1.23.8")
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable")

    testImplementation("io.gitlab.arturbosch.detekt:detekt-api:1.23.8")
    testImplementation("io.gitlab.arturbosch.detekt:detekt-test:1.23.8")
    testImplementation(kotlin("test"))
}
