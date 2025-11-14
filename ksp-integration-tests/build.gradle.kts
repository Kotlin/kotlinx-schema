plugins {
    kotlin("jvm")
    alias(libs.plugins.google.ksp)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

dependencies {
    implementation(project(":kotlinx-schema-annotations"))
    implementation(libs.kotlinx.serialization.json)

    // Third-party annotation libraries for testing description extraction
    implementation(libs.koog.agents.tools)
    implementation(libs.jackson.annotations)
    implementation(libs.langchain4j.core)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.json)

    // KSP processor
    ksp(project(":kotlinx-schema-ksp"))
}

// KSP generates sources to build/generated/ksp/main/kotlin
// This is automatically added to the source sets by KSP plugin
kotlin.sourceSets.main {
    kotlin.srcDir("build/generated/ksp/main/kotlin")
}
