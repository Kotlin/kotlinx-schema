plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") apply true
    `dokka-convention`
}

dokka {
    dokkaSourceSets.configureEach {
    }
}

kotlin {
    jvmToolchain(17)

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

    explicitApi()

    compilerOptions {
        javaParameters = true
        optIn.set(listOf("kotlinx.serialization.ExperimentalSerializationApi"))
    }
}
