plugins {
    kotlin("plugin.serialization") apply true
    `dokka-convention`
    `kotlin-jvm-convention`
}

dokka {
    dokkaSourceSets.configureEach {
    }
}

kotlin {

    dependencies {
        // production dependencies
        api(project(":kotlinx-schema-annotations"))
        api(project(":kotlinx-schema-generator-core"))
        api(libs.kotlinx.serialization.json)

        // test dependencies
        testImplementation(libs.kotlin.test)
        testImplementation(libs.kotest.assertions.core)
        testImplementation(libs.kotest.assertions.json)
    }

    compilerOptions {
        optIn.set(listOf("kotlinx.serialization.ExperimentalSerializationApi"))
    }
}
