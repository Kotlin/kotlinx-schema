plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)

    dependencies {
        // production dependencies
        api(project(":kotlinx-schema-annotations"))

        // test dependencies
        testImplementation(libs.kotlin.test)
        testImplementation(libs.kotest.assertions.core)
        testImplementation(libs.kotest.assertions.json)
    }

    explicitApi()

    compilerOptions {
        javaParameters = true
        optIn.set(listOf("kotlinx.serialization.ExperimentalSerializationApi"))
    }
}