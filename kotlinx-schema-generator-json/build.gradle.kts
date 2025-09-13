plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") apply true
}

kotlin {
    dependencies {
        // production dependencies
        api(project(":kotlinx-schema-annotations"))
        api(libs.kotlinx.serialization.json)

        // test dependencies
        testImplementation(libs.kotlin.test)
        testImplementation(libs.kotest.assertions.json)
    }

    explicitApi()
}